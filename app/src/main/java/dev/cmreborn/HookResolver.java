package dev.cmreborn;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.os.Build;
import android.os.SystemClock;
import android.util.AtomicFile;

import org.luckypray.dexkit.DexKitBridge;
import org.luckypray.dexkit.query.FindClass;
import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.query.matchers.ClassMatcher;
import org.luckypray.dexkit.query.matchers.FieldMatcher;
import org.luckypray.dexkit.query.matchers.FieldsMatcher;
import org.luckypray.dexkit.query.matchers.MethodMatcher;
import org.luckypray.dexkit.query.matchers.MethodsMatcher;
import org.luckypray.dexkit.result.ClassData;
import org.luckypray.dexkit.result.MethodData;
import org.luckypray.dexkit.result.FieldData;
import org.luckypray.dexkit.result.UsingFieldData;
import org.luckypray.dexkit.wrap.DexField;
import org.luckypray.dexkit.wrap.DexMethod;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.function.Consumer;
import java.util.zip.ZipFile;

/**
 * Resolves semantic hook targets before any Messages hooks are installed.
 * Strings/resources identify behavior; signatures and relationships disambiguate it.
 * Obfuscated names are output, never inputs to a scan. Every result must be unique.
 */
final class HookResolver {
    private static final int SCHEMA = 2;
    private static final String CONVERSATION_ID =
            "com.google.android.apps.messaging.shared.datamodel.data.datatypes.ConversationIdType";
    private static final String MESSAGE_ID =
            "com.google.android.apps.messaging.shared.datamodel.data.datatypes.MessageIdType";
    private static final String FUTURE = "com.google.common.util.concurrent.ListenableFuture";
    private static final String[] REQUIRED_CLASSES = {"visibility", "collector", "viewConcrete",
            "viewAbstract", "immutableList", "immutableSet", "status", "reason", "archiveApi",
            "metadata", "intent", "selection", "actionProvider"};
    private static final String[] REQUIRED_METHODS = {"profile", "searchHome", "category",
            "collectorSuccess", "collectorFailure", "conversationAdapter", "starredAdapter",
            "videoAdapter", "mediaAdapter", "linkAdapter", "locationAdapter", "suggestion",
            "contactAdapter", "contactTap", "keepPredicate", "archiveSingle", "archiveFlag",
            "archiveList", "metadataUpdate", "metadataRefresh", "selectionUpdate"};
    private static final String[] REQUIRED_FIELDS = {"viewStarred", "viewConversations",
            "viewNoMatching", "viewSemantic"};
    private final Properties entries = new Properties();
    private final ClassLoader loader;
    private final Consumer<String> log;
    private final List<DexKitBridge> bridges = new ArrayList<>();
    private final Context context;
    private long version;
    private String baseDigest = "";
    private final boolean discoveryOnly = BuildConfig.DEBUG && BuildConfig.DISCOVERY_ONLY;

    private HookResolver(Context context, Consumer<String> log) {
        this.context = context;
        this.loader = context.getClassLoader();
        this.log = log;
    }

    static HookResolver resolve(Context context, Consumer<String> log) {
        HookResolver resolver = new HookResolver(context, log);
        resolver.load();
        return resolver;
    }

    private void load() {
        long started = SystemClock.elapsedRealtime();
        try {
            PackageInfo pkg = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            version = Build.VERSION.SDK_INT >= 28 ? pkg.getLongVersionCode() : pkg.versionCode;
            List<String> paths = new ArrayList<>();
            paths.add(context.getApplicationInfo().sourceDir);
            String[] splits = context.getApplicationInfo().splitSourceDirs;
            if (splits != null) {
                List<String> sorted = new ArrayList<>(Arrays.asList(splits));
                Collections.sort(sorted);
                paths.addAll(sorted);
            }
            List<String> digests = new ArrayList<>();
            for (String path : paths) digests.add(sha256(new File(path)));
            baseDigest = digests.get(0);
            String key = ResolutionPolicy.cacheKey(SCHEMA, BuildConfig.VERSION_NAME, version, digests);
            AtomicFile cache = new AtomicFile(new File(context.getCodeCacheDir(),
                    "cmreborn-hooks.properties"));
            if (readCache(cache, key)) {
                log.accept("discovery cache hit; targets=" + targetCount()
                        + "; elapsedMs=" + (SystemClock.elapsedRealtime() - started));
                return;
            }
            System.loadLibrary("dexkit");
            for (String path : paths) {
                try (ZipFile zip = new ZipFile(path)) {
                    if (zip.getEntry("classes.dex") != null) bridges.add(DexKitBridge.create(path));
                }
            }
            if (bridges.isEmpty()) throw new IllegalStateException("No code APK");
            scan();
            entries.setProperty("cacheKey", key);
            if (complete(entries)) writeCache(cache);
            else log.accept("discovery incomplete; cache not saved; retrying on next process start");
            log.accept("discovery cold scan complete; targets=" + targetCount()
                    + "; elapsedMs=" + (SystemClock.elapsedRealtime() - started)
                    + "; discoveryOnly=" + discoveryOnly);
        } catch (Throwable error) {
            log.accept("discovery unavailable: " + error.getClass().getSimpleName());
        } finally {
            for (DexKitBridge bridge : bridges) {
                try { bridge.close(); } catch (Throwable ignored) { }
            }
            bridges.clear();
        }
    }

    private void scan() {
        // Independent groups continue resolving when another fingerprint disappears.
        attempt("profile", () -> {
            MethodData method = uniqueMethod("profile", MethodMatcher.create()
                    .paramCount(0).usingStrings("Clicked Archived messages"));
            if (method == null) return;
            saveMethod("profile", method, "a");
            Class<?> actionType = method.getMethodInstance(loader).getReturnType();
            List<Class<?>> visibility = new ArrayList<>();
            for (Method getter : actionType.getDeclaredMethods()) {
                if (getter.getParameterCount() != 0 || Modifier.isStatic(getter.getModifiers())) continue;
                Class<?> type = getter.getReturnType();
                if (type.isPrimitive() || type.getName().startsWith("java.")) continue;
                try {
                    type.getDeclaredConstructor();
                    int booleans = 0;
                    int setters = 0;
                    for (Field f : type.getDeclaredFields()) {
                        if (!Modifier.isStatic(f.getModifiers()) && f.getType() == boolean.class) booleans++;
                    }
                    for (Method m : type.getDeclaredMethods()) {
                        if (Arrays.equals(m.getParameterTypes(), new Class<?>[]{boolean.class})
                                && m.getReturnType() == void.class) setters++;
                    }
                    if (booleans == 1 && setters == 1) visibility.add(type);
                } catch (ReflectiveOperationException ignored) { }
            }
            Class<?> type = unique("visibility", visibility);
            if (type != null) saveClass("visibility", type.getName());
        });
        attempt("searchHome", () -> {
            int id = resource("layout", "zero_state_search_fragment");
            if (id == 0) return;
            saveMethod("searchHome", uniqueMethod("searchHome", MethodMatcher.create()
                    .returnType("android.view.View")
                    .paramTypes("android.view.LayoutInflater", "android.view.ViewGroup", "android.os.Bundle")
                    .usingNumbers(id)), "N", "M");
        });
        attempt("category", () -> {
            ClassData owner = uniqueClass("categoryOwner", ClassMatcher.create()
                    .usingStrings("CATEGORY_CONTENT_DATA_SOURCE_KEY"));
            if (owner == null) return;
            saveMethod("category", uniqueMethod("category", MethodMatcher.create()
                    .declaredClass(ClassMatcher.create().fields(FieldsMatcher.create()
                            .add(FieldMatcher.create().type(owner.getName()))))
                    .paramCount(0).returnType(FUTURE)), "b");
        });
        attempt("collector", () -> {
            ClassData c = uniqueClass("collector", ClassMatcher.create()
                    .usingStrings("IcingSearchApiImpl$1", "cannot get the id for result."));
            if (c == null) return;
            saveClass("collector", c.getName());
            List<MethodData> success = new ArrayList<>();
            List<MethodData> failure = new ArrayList<>();
            for (MethodData m : c.getMethods()) {
                if (!m.isMethod() || !"void".equals(m.getReturnTypeName())) continue;
                if (m.getParamCount() == 2 && "java.lang.String".equals(m.getParamTypeNames().get(0))) failure.add(m);
            }
            MethodData fail = unique("collectorFailure", failure);
            if (fail == null) return;
            String callback = fail.getParamTypeNames().get(1);
            for (MethodData m : c.getMethods()) {
                if (m.isMethod() && "void".equals(m.getReturnTypeName())
                        && m.getParamTypeNames().equals(Collections.singletonList(callback))) success.add(m);
            }
            saveMethod("collectorSuccess", unique("collectorSuccess", success), "c");
            saveMethod("collectorFailure", fail, "b");
        });
        traceMethod("conversationAdapter", "SearchConversationListAdapter#updateResults", "F", null);
        traceMethod("starredAdapter", "StarredMessagesAdapter#updateResults", "F", null);
        traceMethod("videoAdapter", "VideosAdapter#updateResults", "G", "java.util.List");
        traceMethod("mediaAdapter", "MediaAdapter#updateResults", "G", "java.util.List");
        traceMethod("linkAdapter", "LinksAdapter#updateResults", "M", "java.util.List");
        traceMethod("locationAdapter", "LocationsAdapter#updateResults", "M", "java.util.List");
        attempt("viewData", () -> {
            ClassData concrete = uniqueClass("viewConcrete", ClassMatcher.create()
                    .usingStrings("ConversationSearchViewData{starredTextResultItemData="));
            if (concrete == null) return;
            Method conv = method("conversationAdapter");
            Method star = method("starredAdapter");
            if (conv == null || star == null || conv.getParameterTypes()[0] != star.getParameterTypes()[0]) return;
            Class<?> abstractType = conv.getParameterTypes()[0];
            Class<?> actual = concrete.getInstance(loader);
            if (actual.getSuperclass() != abstractType) return;
            List<Constructor<?>> matches = new ArrayList<>();
            for (Constructor<?> ctor : actual.getDeclaredConstructors()) {
                Class<?>[] p = ctor.getParameterTypes();
                if (p.length == 4 && p[0] == p[1] && p[0] == p[3]
                        && List.class.isAssignableFrom(p[0]) && Set.class.isAssignableFrom(p[2])) matches.add(ctor);
            }
            Constructor<?> ctor = unique("viewConstructor", matches);
            if (ctor == null) return;
            saveClass("viewConcrete", actual.getName());
            saveClass("viewAbstract", abstractType.getName());
            saveClass("immutableList", ctor.getParameterTypes()[0].getName());
            saveClass("immutableSet", ctor.getParameterTypes()[2].getName());
            MethodData convData = methodData("conversationAdapter");
            MethodData starData = methodData("starredAdapter");
            saveReadField("viewConversations", convData, actual.getName(), ctor.getParameterTypes()[0].getName());
            saveReadField("viewNoMatching", convData, actual.getName(), ctor.getParameterTypes()[2].getName());
            saveReadField("viewStarred", starData, actual.getName(), ctor.getParameterTypes()[0].getName());
            Set<String> used = new LinkedHashSet<>();
            for (String role : new String[]{"viewConversations", "viewNoMatching", "viewStarred"}) {
                String descriptor = entries.getProperty("field." + role);
                if (descriptor != null) used.add(descriptor);
            }
            if (used.size() == 3) {
                List<FieldData> remaining = new ArrayList<>();
                for (FieldData field : concrete.getFields()) {
                    if (!Modifier.isStatic(field.getModifiers()) && !used.contains(field.getDescriptor())
                            && field.getTypeName().equals(ctor.getParameterTypes()[0].getName())) remaining.add(field);
                }
                FieldData semantic = unique("viewSemantic", remaining);
                if (semantic != null) entries.setProperty("field.viewSemantic", semantic.getDescriptor());
            }
        });
        attempt("suggestion", () -> saveMethod("suggestion", uniqueMethod("suggestion",
                MethodMatcher.create().returnType("android.widget.Filter$FilterResults")
                        .paramTypes("java.lang.CharSequence")
                        .usingStrings("Failed to get suggestions.")), "performFiltering"));
        attempt("contactAdapter", () -> {
            ClassData c = uniqueClass("contactAdapter", ClassMatcher.create()
                    .usingStrings("ContactsAdapter onBindViewHolder"));
            if (c != null) shapeMethod("contactAdapter", c, "void", new String[]{"java.util.List"}, "m");
        });
        attempt("contactTap", () -> {
            ClassData c = uniqueClass("contactTap", ClassMatcher.create()
                    .usingStrings("Failed to check if the contact is a participant. Starting 1-1 conversation."));
            if (c != null) saveMethod("contactTap", uniqueMethod("contactTap",
                    MethodMatcher.create().declaredClass(c.getName()).returnType("void")
                            .paramTypes("java.lang.Object", "java.lang.Object")), "c");
        });
        attempt("status", () -> {
            ClassData c = uniqueClass("status", ClassMatcher.create().superClass("java.lang.Enum")
                    .usingStrings("UNARCHIVED", "ARCHIVED", "KEEP_ARCHIVED", "Invalid ArchiveStatus value:"));
            if (c == null) return;
            saveClass("status", c.getName());
            String keepField = null;
            Class<?> enumClass = c.getInstance(loader);
            for (Field field : enumClass.getDeclaredFields()) {
                if (!Modifier.isStatic(field.getModifiers()) || field.getType() != enumClass) continue;
                field.setAccessible(true);
                Object constant = field.get(null);
                if (constant instanceof Enum<?> && "KEEP_ARCHIVED".equals(((Enum<?>) constant).name())) {
                    if (keepField != null) throw new IllegalStateException("Duplicate enum constant field");
                    keepField = field.getName();
                }
            }
            if (keepField == null) return;
            saveMethod("keepPredicate", uniqueMethod("keepPredicate", MethodMatcher.create()
                    .declaredClass(c.getName()).returnType("boolean").paramCount(0)
                    .addUsingField(FieldMatcher.create().declaredClass(c.getName()).name(keepField))), "g");
        });
        attempt("reason", () -> {
            ClassData c = uniqueClass("reason", ClassMatcher.create().superClass("java.lang.Enum")
                    .usingStrings("CONVERSATION_FROM_LIST", "CONVERSATION_FROM_SEARCH", "CONVERSATION_FROM_DETAILS_ACTION"));
            if (c != null) saveClass("reason", c.getName());
        });
        attempt("archiveApi", () -> {
            String status = className("status"), reason = className("reason"), list = className("immutableList");
            if (status == null || reason == null || list == null) return;
            ClassData c = uniqueClass("archiveApi", ClassMatcher.create()
                    .usingStrings("com/google/android/apps/messaging/shared/util/archive/ArchiveApiImpl")
                    .methods(MethodsMatcher.create().add(MethodMatcher.create().returnType("boolean")
                            .paramTypes(CONVERSATION_ID, status, reason, "boolean"))));
            if (c == null) return;
            saveClass("archiveApi", c.getName());
            shapeMethod("archiveSingle", c, "boolean", new String[]{CONVERSATION_ID, status, reason}, "a");
            shapeMethod("archiveFlag", c, "boolean", new String[]{CONVERSATION_ID, status, reason, "boolean"}, "b");
            shapeMethod("archiveList", c, list, new String[]{list, status, reason}, "c");
        });
        attempt("metadata", () -> {
            String status = className("status");
            if (status == null) return;
            ClassData c = uniqueClass("metadata", ClassMatcher.create()
                    .usingStrings("ConversationMetadataDatabaseOperations#refreshConversationMetadata")
                    .methods(MethodsMatcher.create().add(MethodMatcher.create().returnType("void")
                            .paramTypes(CONVERSATION_ID, "boolean", status))));
            if (c == null) return;
            saveClass("metadata", c.getName());
            shapeMethod("metadataUpdate", c, "void", new String[]{CONVERSATION_ID, MESSAGE_ID,
                    "java.lang.Long", status, "java.lang.String", "boolean", "long", "java.lang.Integer",
                    "com.google.android.apps.messaging.shared.api.messaging.selfidentity.SelfIdentityId", "boolean"}, "h", "j", "i");
            saveMethod("metadataRefresh", uniqueMethod("metadataRefresh", MethodMatcher.create()
                    .declaredClass(c.getName()).returnType("void").paramTypes(CONVERSATION_ID, "boolean", status)
                    .usingStrings("ConversationMetadataDatabaseOperations#refreshConversationMetadata")), "b");
        });
        attempt("intent", () -> {
            ClassData c = uniqueClass("intent", ClassMatcher.create()
                    .usingStrings("AccountId was manually propagated. Use AccountIntents instead."));
            if (c != null) saveClass("intent", c.getName());
        });
        attempt("selection", () -> {
            ClassData c = uniqueClass("selection", ClassMatcher.create()
                    .usingStrings("Attempted to execute add contact action when the number of selected messages is %d."));
            if (c == null || !android.view.ActionMode.Callback.class.isAssignableFrom(c.getInstance(loader))) return;
            saveClass("selection", c.getName());
            // Visibility recomputation is the only private no-argument void method.
            saveMethod("selectionUpdate", uniqueMethod("selectionUpdate", MethodMatcher.create()
                    .declaredClass(c.getName()).modifiers(Modifier.PRIVATE).returnType("void").paramCount(0)), "b");
        });
        attempt("actionProvider", () -> {
            int archive = resource("string", "action_archive");
            int unarchive = resource("string", "action_unarchive");
            Method predicate = method("keepPredicate");
            if (archive == 0 || unarchive == 0 || predicate == null) return;
            MethodData m = uniqueMethod("actionProvider", MethodMatcher.create()
                    .returnType("java.lang.Object").paramCount(2).usingNumbers(archive, unarchive)
                    .addInvoke(new DexMethod(predicate).serialize()));
            if (m != null) saveClass("actionProvider", m.getClassName());
        });
    }

    private void traceMethod(String role, String trace, String alias, String parameter) {
        attempt(role, () -> {
            MethodMatcher matcher = MethodMatcher.create().returnType("void").paramCount(1).usingStrings(trace);
            if (parameter != null) matcher.paramTypes(parameter);
            saveMethod(role, uniqueMethod(role, matcher), alias);
        });
    }

    private MethodData methodData(String role) {
        String descriptor = entries.getProperty("method." + role);
        if (descriptor == null) return null;
        for (DexKitBridge bridge : bridges) {
            MethodData data = bridge.getMethodData(descriptor);
            if (data != null) return data;
        }
        return null;
    }

    private void saveReadField(String role, MethodData method, String owner, String type) {
        if (method == null) return;
        Map<String, FieldData> fields = new LinkedHashMap<>();
        for (UsingFieldData use : method.getUsingFields()) {
            FieldData field = use.getField();
            if (owner.equals(field.getClassName()) && type.equals(field.getTypeName())
                    && !Modifier.isStatic(field.getModifiers())) fields.put(field.getDescriptor(), field);
        }
        FieldData field = unique(role, new ArrayList<>(fields.values()));
        if (field != null) entries.setProperty("field." + role, field.getDescriptor());
    }

    Object field(String role, Object target) throws Exception {
        String descriptor = entries.getProperty("field." + role);
        if (descriptor == null) throw new NoSuchFieldException(role);
        Field field = new DexField(descriptor).getFieldInstance(loader);
        field.setAccessible(true);
        return field.get(target);
    }

    boolean hasViewFields() {
        return entries.containsKey("field.viewStarred") && entries.containsKey("field.viewConversations")
                && entries.containsKey("field.viewNoMatching") && entries.containsKey("field.viewSemantic");
    }

    private void shapeMethod(String role, ClassData owner, String returnType,
            String[] parameters, String... aliases) throws Exception {
        saveMethod(role, uniqueMethod(role, MethodMatcher.create().declaredClass(owner.getName())
                .returnType(returnType).paramTypes(parameters)), aliases);
    }

    private int resource(String type, String name) {
        return context.getResources().getIdentifier(name, type, context.getPackageName());
    }

    private ClassData uniqueClass(String role, ClassMatcher matcher) {
        Map<String, ClassData> candidates = new LinkedHashMap<>();
        for (DexKitBridge bridge : bridges) {
            for (ClassData c : bridge.findClass(FindClass.create().matcher(matcher))) candidates.put(c.getName(), c);
        }
        if (candidates.size() > 1) entries.setProperty("ambiguous." + role, "true");
        if (candidates.size() != 1) log.accept("discovery " + role + " class matches=" + candidates.size());
        return candidates.size() == 1 ? candidates.values().iterator().next() : null;
    }

    private MethodData uniqueMethod(String role, MethodMatcher matcher) {
        Map<String, MethodData> candidates = new LinkedHashMap<>();
        for (DexKitBridge bridge : bridges) {
            for (MethodData m : bridge.findMethod(FindMethod.create().matcher(matcher))) candidates.put(m.getDescriptor(), m);
        }
        if (candidates.size() > 1) entries.setProperty("ambiguous." + role, "true");
        if (candidates.size() != 1) log.accept("discovery " + role + " method matches=" + candidates.size());
        return candidates.size() == 1 ? candidates.values().iterator().next() : null;
    }

    private <T> T unique(String role, List<T> candidates) {
        T result = ResolutionPolicy.unique(candidates);
        if (new LinkedHashSet<>(candidates).size() > 1) entries.setProperty("ambiguous." + role, "true");
        if (result == null) log.accept("discovery " + role + " shape matches=" + candidates.size());
        return result;
    }

    private void saveClass(String role, String name) {
        entries.setProperty("class." + role, name);
        log.accept("discovery resolved " + role + "=" + name);
    }

    private void saveMethod(String role, MethodData data, String... aliases) throws Exception {
        if (data == null) return;
        Method method = data.getMethodInstance(loader);
        entries.setProperty("method." + role, data.getDescriptor());
        saveClass(role, data.getClassName());
        for (String alias : aliases) entries.setProperty(aliasKey(method.getDeclaringClass(), alias,
                method.getParameterTypes()), data.getDescriptor());
        entries.setProperty(aliasKey(method.getDeclaringClass(), method.getName(),
                method.getParameterTypes()), data.getDescriptor());
        log.accept("discovery member " + role + "=" + data.getDescriptor());
    }

    private interface Scan { void run() throws Exception; }

    private void attempt(String role, Scan scan) {
        try { scan.run(); } catch (Throwable failure) {
            log.accept("discovery " + role + " failed: " + failure.getClass().getSimpleName());
        }
    }

    String className(String role) { return entries.getProperty("class." + role); }

    Method method(String role) {
        String descriptor = entries.getProperty("method." + role);
        if (descriptor == null) return null;
        try {
            Method method = new DexMethod(descriptor).getMethodInstance(loader);
            method.setAccessible(true);
            return method;
        } catch (Throwable ignored) { return null; }
    }

    String[] classes(String role, String[] fallback) {
        String resolved = className(role);
        if (resolved != null) return new String[]{resolved};
        if (fallbackAllowed(role) && fallback.length > 0) {
            log.accept("discovery exact-APK fallback: " + role);
            return new String[]{fallback[0]};
        }
        log.accept("discovery unresolved; hook disabled: " + role);
        return new String[0];
    }

    String[] adapter(String role, String fallback) {
        Method method = method(role);
        if (method != null) return new String[]{method.getDeclaringClass().getName() + "#" + method.getName()};
        return fallbackAllowed(role) ? new String[]{fallback} : new String[0];
    }

    boolean fallbackAllowed(String role) {
        // An ambiguous dependency must not regain a literal-name hook via a caller's alias.
        boolean ambiguous = false;
        for (String key : entries.stringPropertyNames()) {
            if (key.startsWith("ambiguous.") && Boolean.parseBoolean(entries.getProperty(key))) {
                ambiguous = true;
                break;
            }
        }
        return ResolutionPolicy.allowFallback(discoveryOnly,
                ambiguous, version, baseDigest);
    }

    Method hookMethod(Class<?> owner, String legacyName, Class<?>[] parameters) throws Exception {
        String descriptor = entries.getProperty(aliasKey(owner, legacyName, parameters));
        if (descriptor != null) return new DexMethod(descriptor).getMethodInstance(loader);
        // Android lifecycle and Filter method names are API contracts, not offsets.
        if (legacyName.startsWith("on") || "performFiltering".equals(legacyName)
                || owner.getName().startsWith("android.") || fallbackAllowed("member")) {
            return owner.getDeclaredMethod(legacyName, parameters);
        }
        throw new NoSuchMethodException("No validated semantic method for " + owner.getName());
    }

    private static String aliasKey(Class<?> owner, String name, Class<?>[] params) {
        StringBuilder key = new StringBuilder("alias.").append(owner.getName()).append('#').append(name);
        for (Class<?> type : params) key.append(':').append(type.getName());
        return key.toString();
    }

    private int targetCount() {
        int count = 0;
        for (String key : entries.stringPropertyNames()) if (key.startsWith("class.")) count++;
        return count;
    }

    private boolean readCache(AtomicFile cache, String key) {
        try (FileInputStream in = cache.openRead()) {
            Properties cached = new Properties();
            cached.load(in);
            if (!key.equals(cached.getProperty("cacheKey"))) return false;
            if (!complete(cached)) return false;
            for (String name : cached.stringPropertyNames()) {
                if (name.startsWith("class.")) Class.forName(cached.getProperty(name), false, loader);
                if (name.startsWith("method.") || name.startsWith("alias.")) {
                    new DexMethod(cached.getProperty(name)).getMethodInstance(loader);
                }
                if (name.startsWith("field.")) new DexField(cached.getProperty(name)).getFieldInstance(loader);
            }
            entries.putAll(cached);
            return true;
        } catch (Throwable ignored) { return false; }
    }

    private static boolean complete(Properties values) {
        for (String role : REQUIRED_CLASSES) if (!values.containsKey("class." + role)) return false;
        for (String role : REQUIRED_METHODS) if (!values.containsKey("method." + role)) return false;
        for (String role : REQUIRED_FIELDS) if (!values.containsKey("field." + role)) return false;
        for (String key : values.stringPropertyNames()) if (key.startsWith("ambiguous.")) return false;
        return true;
    }

    private void writeCache(AtomicFile cache) {
        FileOutputStream out = null;
        try {
            out = cache.startWrite();
            entries.store(out, "CMReborn semantic hook cache; no conversation data");
            cache.finishWrite(out);
        } catch (Throwable ignored) {
            if (out != null) cache.failWrite(out);
        }
    }

    private static String sha256(File apk) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (FileInputStream in = new FileInputStream(apk)) {
            byte[] bytes = new byte[128 * 1024];
            int count;
            while ((count = in.read(bytes)) != -1) digest.update(bytes, 0, count);
        }
        StringBuilder hex = new StringBuilder(64);
        for (byte b : digest.digest()) hex.append(String.format(java.util.Locale.ROOT, "%02x", b & 255));
        return hex.toString();
    }
}
