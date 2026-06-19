package dev.cmreborn;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public final class CMRebornHook implements IXposedHookLoadPackage {
    private static final String TAG = "CMReborn";
    private static final boolean ENABLE_DEBUG_LOGS = BuildConfig.DEBUG;
    private static final boolean ENABLE_ERROR_LOGS = true;
    private static final boolean ENABLE_RUNTIME_PROBE = false;
    // Query-stage archive exclusion can over-filter after upstream schema changes.
    // Keep disabled unless verified against the current Messages build.
    private static final String TARGET_PACKAGE = "com.google.android.apps.messaging";
    private static final long LOG_FILE_MAX_BYTES = 1024L * 1024L;
    private static final String RUNTIME_LOG_FILE_NAME = "cmreborn_runtime.log";
    private static final String ARCHIVED_ACTIVITY =
            "com.google.android.apps.messaging.home.ArchivedActivity";
    private static final String MAIN_ACTIVITY =
            "com.google.android.apps.messaging.main.MainActivity";
    private static final String ZERO_STATE_SEARCH_ACTIVITY =
            "com.google.android.apps.messaging.ui.search.ZeroStateSearchActivity";
    private static final String ZERO_STATE_SEARCH_BOX_CLASS =
            "com.google.android.apps.messaging.ui.search.ZeroStateSearchBox";
    private static final String ARCHIVED_FOLDER_ENUM_NAME = "ARCHIVED";
    private static final String SEARCH_TRIGGER = "helloworld";
    private static final int INSPECTED_ACTION_SHOW_ARCHIVED_ID = 0x7f0b00ff;
    private static final int INSPECTED_ACTION_UNARCHIVE_ID = 0x7f0b0105;
    private static final int ARCHIVE_STATUS_UNARCHIVED = 0;
    private static final int ARCHIVE_STATUS_ARCHIVED = 1;
    private static final int ARCHIVE_STATUS_KEEP_ARCHIVED = 2;
    private static final int ARCHIVE_STATUS_UNKNOWN = Integer.MIN_VALUE;
    private static final int MAX_ONCE_LOG_KEYS = 512;
    private static final int MAX_RESOURCE_ID_CACHE_SIZE = 512;
    private static final int MAX_CHANNEL_IMPORTANCE_CACHE_SIZE = 2048;
    private static final long TRIGGER_THROTTLE_MS = 1500L;
    // Validated on Google Messages 308183063 (RC02), 309541063 (RC03), 310684063 (RC00),
    // 310772063 (RC00), 311460063 (RC01), and 311755063 (RC00).
    private static final String[] PROFILE_ARCHIVED_ACTION_PROVIDER_CLASS_CANDIDATES =
            {"aldv", "aldm", "akzi", "aksj", "akku", "akfq"};
    private static final String[] PROFILE_HIDDEN_VISIBILITY_CLASS_CANDIDATES =
            {"etql", "alhw", "alds", "akwt", "akpe", "akka"};
    private static final String[] SEARCH_HOME_FRAGMENT_CLASS_CANDIDATES =
            {"dubk", "dtuv", "dtje", "dsyx", "drlp", "dqlb"};
    private static final String[] SEARCH_CATEGORY_PROVIDER_CLASS_CANDIDATES =
            {"ducq", "dtwb", "dtkk", "dtad", "drmv", "dqmh"};
    private static final String[] SEARCH_VIEW_DATA_ABSTRACT_CLASS_CANDIDATES =
            {"duej", "dtxu", "dtmd", "dtbw", "droo", "dqoa"};
    private static final String[] SEARCH_VIEW_DATA_CONCRETE_CLASS_CANDIDATES =
            {"duec", "dtxn", "dtlw", "dtbp", "droh", "dqnt"};
    private static final String[] SEARCH_CONVERSATION_RESULTS_ADAPTER_CLASS_CANDIDATES =
            {"duiu", "ducf", "dtqo", "dtgh", "drsy", "dqsk"};
    private static final String[] SEARCH_STARRED_RESULTS_ADAPTER_CLASS_CANDIDATES =
            {"duja", "ducl", "dtqu", "dtgn", "drte", "dqsq"};
    private static final String[] SEARCH_SUGGESTION_FILTER_CLASS_CANDIDATES =
            {"ducf", "dtvq", "dtjz", "dszs", "drmk", "dqlw"};
    private static final String[] SEARCH_CONTACT_RESULTS_ADAPTER_METHOD_CANDIDATES =
            {"dufz#m", "dtzk#m", "dtnt#m", "dtdm#m", "drqd#l", "dqpp#l"};
    private static final String[] SEARCH_CONTACT_TAP_HANDLER_CLASS_CANDIDATES =
            {"dubv", "dtvg", "dtjp", "dszi", "drma", "dqlm"};
    private static final String[] ATTACHMENT_RESULT_ADAPTER_METHOD_CANDIDATES = {
            "duhl#G", "dujr#G", "duhk#M", "dugw#M",
            "duaw#G", "dudc#G", "duav#M", "duah#M",
            "dtpf#G", "dtrl#G", "dtpe#M", "dtoq#M",
            "dtey#G", "dthe#G", "dtex#M", "dtej#M",
            "drrp#G", "drtv#G", "drro#M", "drra#M",
            "dqrb#G", "dqth#G", "dqra#M", "dqqm#M"
    };
    private static final String[] IMMUTABLE_LIST_CLASS_CANDIDATES =
            {"fgkq", "fgfd", "fgpr", "fgdq", "feml", "fdzc"};
    private static final String[] IMMUTABLE_SET_CLASS_CANDIDATES =
            {"fgmk", "fggx", "fgrl", "fgfk", "feof", "feaw"};
    private static final String[] ARCHIVE_STATUS_ENUM_CLASS_CANDIDATES =
            {"clan", "ckya", "cknk", "ckdj", "cjcn", "cikq"};
    private static final String[] ARCHIVE_REASON_CLASS_CANDIDATES =
            {"fhnv", "fhii", "fhuj", "ffrd", "ffdu"};
    private static final String[] ARCHIVE_ID_LIST_CLASS_CANDIDATES =
            {"fgkq", "fgfd", "fgpr", "fgdq", "feml", "fdzc"};
    private static final String[] ARCHIVE_API_IMPL_CLASS_CANDIDATES =
            {"djfw", "djae", "diou", "dieo", "dgtz", "dfwt"};
    private static final String[] CONVERSATION_METADATA_OPS_CLASS_CANDIDATES =
            {"bpoi", "bplw", "bpcv", "bosz", "bnsq", "bmuo"};
    private static final String[] ARCHIVE_INTENT_HELPER_CLASS_CANDIDATES =
            {"fbzo", "fbtu", "fceb", "fbsj", "fabf", "ezny"};
    private static final String[] ARCHIVED_SELECTION_CONTROLLER_CLASS_CANDIDATES =
            {"dqxs", "dqra", "dqfp", "dpvi", "dojd", "dniq"};

    private static final Set<ClassLoader> INSTALLED_CLASSLOADERS =
            Collections.newSetFromMap(new WeakHashMap<ClassLoader, Boolean>());
    private static final Set<String> ONCE_LOG_KEYS =
            Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());
    private static final Map<String, Integer> RESOURCE_ID_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, Integer> CHANNEL_IMPORTANCE_CACHE =
            new ConcurrentHashMap<>();
    private static volatile boolean attachHookInstalled;
    private static volatile boolean menuHookInstalled;
    private static volatile boolean overflowSelectionHookInstalled;
    private static volatile boolean archiveBackToInboxPendingFromTrigger;
    private static volatile long lastTriggerAtMs;
    private static volatile Class<?> archiveIntentAccountHelperClass;
    private static volatile WeakReference<Context> runtimeContextRef =
            new WeakReference<>(null);
    private static volatile File runtimeLogDirectory;
    private static volatile boolean runtimeProbeToastShown;
    private static final ThreadLocal<Integer> USER_UNARCHIVE_ACTION_DEPTH = new ThreadLocal<>();

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        if (!TARGET_PACKAGE.equals(lpparam.packageName)) {
            return;
        }

        if (ENABLE_DEBUG_LOGS) {
            log("module loaded; package matched; process=" + lpparam.processName);
        }
        if (ENABLE_RUNTIME_PROBE) {
            hookRuntimeProbe();
        }
        hookOverflowArchivedMenu();
        hookApplicationAttach();
    }

    private static void hookRuntimeProbe() {
        try {
            XposedHelpers.findAndHookMethod(Activity.class, "onResume", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    Activity activity = (Activity) param.thisObject;
                    if (activity == null || !TARGET_PACKAGE.equals(activity.getPackageName())) {
                        return;
                    }
                    updateRuntimeContext(activity);
                    appendRuntimeLog("probe onResume activity=" + activity.getClass().getName());
                    if (runtimeProbeToastShown) {
                        return;
                    }
                    runtimeProbeToastShown = true;
                    try {
                        Toast.makeText(activity, "CMReborn hook active", Toast.LENGTH_SHORT).show();
                    } catch (Throwable ignored) {
                        // Ignore UI probe failures.
                    }
                }
            });
            appendRuntimeLog("runtime probe hook installed: Activity.onResume");
        } catch (Throwable t) {
            logThrowable("hook failed: runtime probe", t);
        }
    }

    private static void hookApplicationAttach() {
        if (attachHookInstalled) {
            return;
        }
        synchronized (CMRebornHook.class) {
            if (attachHookInstalled) {
                return;
            }
            try {
                XposedHelpers.findAndHookMethod(Application.class, "attach", Context.class,
                        new XC_MethodHook() {
                            @Override
                            protected void afterHookedMethod(MethodHookParam param) {
                                Context context = (Context) param.args[0];
                                if (context == null
                                        || !TARGET_PACKAGE.equals(context.getPackageName())) {
                                    return;
                                }

                                ClassLoader classLoader = context.getClassLoader();
                                if (classLoader == null) {
                                    log("target package matched but classLoader was null");
                                    return;
                                }
                                updateRuntimeContext(context);
                                appendRuntimeLog("Application.attach matched target package; process="
                                        + context.getApplicationInfo().processName);

                                synchronized (INSTALLED_CLASSLOADERS) {
                                    if (!INSTALLED_CLASSLOADERS.add(classLoader)) {
                                        return;
                                    }
                                }

                                log("target package matched; installing app hooks");
                                resolveRuntimeClasses(classLoader);
                                Rc03UiHooks.install(classLoader);
                                Rc03SearchHooks.install(classLoader);
                                Rc03ArchivedSelectionHooks.install(classLoader);
                                Rc03ArchiveStateHooks.install(classLoader);
                            }
                        });
                attachHookInstalled = true;
                log("hook installed: Application.attach");
            } catch (Throwable t) {
                logThrowable("hook failed: Application.attach", t);
            }
        }
    }

    private static final class Rc03UiHooks {
        private Rc03UiHooks() {
        }

        static void install(ClassLoader classLoader) {
            log("hook disabled: overflow handler class probe; using Activity guard");
            hookGlobalArchivedMenuSelectionGuard();
            hookProfileArchivedMenu(classLoader);
        }
    }

    private static final class Rc03SearchHooks {
        private Rc03SearchHooks() {
        }

        static void install(ClassLoader classLoader) {
            hookSearchTrigger(classLoader);
            hookSearchHomeCleanup(classLoader);
            hookSearchCategorySuggestions(classLoader);
            log("hook disabled: query-stage archived search filter");
            hookSearchResultAdapters(classLoader);
            hookSearchSuggestionContactFilter(classLoader);
            hookSearchContactResultsFilter(classLoader);
            hookSearchContactTapBehavior(classLoader);
            log("hook disabled: attachment query context and builder fallbacks");
            hookAttachmentResultAdapters(classLoader);
        }
    }

    private static final class Rc03ArchivedSelectionHooks {
        private Rc03ArchivedSelectionHooks() {
        }

        static void install(ClassLoader classLoader) {
            hookArchivedSelectionUnarchiveVisibility(classLoader);
            hookUserUnarchiveActionSignal(classLoader);
            hookArchivedBackToInbox(classLoader);
        }
    }

    private static final class Rc03ArchiveStateHooks {
        private Rc03ArchiveStateHooks() {
        }

        static void install(ClassLoader classLoader) {
            hookArchivePreserve(classLoader);
            hookArchiveNotificationAllowPolicy(classLoader);
        }
    }

    private static void hookOverflowArchivedMenu() {
        if (menuHookInstalled) {
            return;
        }
        synchronized (CMRebornHook.class) {
            if (menuHookInstalled) {
                return;
            }
            try {
                XposedHelpers.findAndHookMethod(Activity.class, "onPrepareOptionsMenu", Menu.class,
                        new XC_MethodHook() {
                            @Override
                            protected void afterHookedMethod(MethodHookParam param) {
                                Activity activity = (Activity) param.thisObject;
                                if (!TARGET_PACKAGE.equals(activity.getPackageName())) {
                                    return;
                                }

                                Menu menu = (Menu) param.args[0];
                                int itemId = resourceId(activity, "id", "action_show_archived");
                                if (itemId == 0) {
                                    return;
                                }

                                MenuItem item = menu.findItem(itemId);
                                if (item != null && item.isVisible()) {
                                    item.setVisible(false);
                                    item.setEnabled(false);
                                    logOnce("archived-overflow-hidden",
                                            "archived overflow menu hidden");
                                }
                            }
                        });
                menuHookInstalled = true;
                log("hook installed: Activity.onPrepareOptionsMenu action_show_archived");
            } catch (Throwable t) {
                logThrowable("hook failed: Activity.onPrepareOptionsMenu", t);
            }
        }
    }

    private static synchronized void appendRuntimeLog(String message) {
        if (!ENABLE_DEBUG_LOGS) {
            return;
        }
        if (TextUtils.isEmpty(message)) {
            return;
        }
        try {
            File dir = runtimeLogDirectory;
            if (dir == null) {
                Context context = getRuntimeContext();
                dir = resolveRuntimeLogDirectory(context);
                if (dir != null) {
                    runtimeLogDirectory = dir;
                }
            }
            if (dir == null) {
                return;
            }
            File logFile = new File(dir, RUNTIME_LOG_FILE_NAME);
            if (logFile.exists() && logFile.length() > LOG_FILE_MAX_BYTES) {
                if (!logFile.delete()) {
                    return;
                }
            }
            FileWriter writer = new FileWriter(logFile, true);
            try {
                writer.write(System.currentTimeMillis() + " | " + message + "\n");
            } finally {
                writer.close();
            }
        } catch (IOException ignored) {
            // Ignore runtime-log failures.
        }
    }

    private static void updateRuntimeContext(Context context) {
        if (context == null) {
            return;
        }
        Context appContext = context.getApplicationContext();
        Context candidate = appContext != null ? appContext : context;
        runtimeContextRef = new WeakReference<>(candidate);
        File dir = resolveRuntimeLogDirectory(candidate);
        if (dir != null) {
            runtimeLogDirectory = dir;
        }
    }

    private static Context getRuntimeContext() {
        WeakReference<Context> ref = runtimeContextRef;
        return ref != null ? ref.get() : null;
    }

    private static File resolveRuntimeLogDirectory(Context context) {
        if (context == null) {
            return null;
        }
        File dir = context.getExternalFilesDir(null);
        if (dir == null) {
            dir = context.getExternalCacheDir();
        }
        if (dir == null) {
            return null;
        }
        if (!dir.exists() && !dir.mkdirs()) {
            return null;
        }
        return dir;
    }

    private static void hookGlobalArchivedMenuSelectionGuard() {
        if (overflowSelectionHookInstalled) {
            return;
        }
        synchronized (CMRebornHook.class) {
            if (overflowSelectionHookInstalled) {
                return;
            }
            try {
                XposedHelpers.findAndHookMethod(Activity.class, "onOptionsItemSelected",
                        MenuItem.class, new XC_MethodHook() {
                            @Override
                            protected void beforeHookedMethod(MethodHookParam param) {
                                Activity activity = (Activity) param.thisObject;
                                if (activity == null
                                        || !TARGET_PACKAGE.equals(activity.getPackageName())) {
                                    return;
                                }
                                MenuItem item = param.args != null && param.args.length > 0
                                        ? (MenuItem) param.args[0] : null;
                                if (item == null) {
                                    return;
                                }
                                if (isArchivedMenuItemSelection(activity, item)) {
                                    log("blocked archived overflow menu selection via Activity fallback");
                                    param.setResult(true);
                                }
                            }
                        });
                overflowSelectionHookInstalled = true;
                log("hook installed: Activity.onOptionsItemSelected(MenuItem) archived fallback");
            } catch (Throwable t) {
                logThrowable("hook failed: Activity.onOptionsItemSelected archived fallback", t);
            }
        }
    }

    private static boolean isArchivedMenuItemSelection(Activity activity, MenuItem item) {
        if (activity == null || item == null) {
            return false;
        }
        int itemId = item.getItemId();
        if (itemId == INSPECTED_ACTION_SHOW_ARCHIVED_ID) {
            return true;
        }
        int archiveId = resourceId(activity, "id", "action_show_archived");
        return archiveId != 0 && itemId == archiveId;
    }

    private static void hookProfileArchivedMenu(ClassLoader classLoader) {
        final Class<?> hiddenVisibilityHandlerClass = findInstantiableClassAny(classLoader,
                PROFILE_HIDDEN_VISIBILITY_CLASS_CANDIDATES);
        if (hiddenVisibilityHandlerClass == null) {
            log("hook unavailable: profile hidden visibility class not found");
            return;
        }
        boolean hookedAny = false;
        for (String className : PROFILE_ARCHIVED_ACTION_PROVIDER_CLASS_CANDIDATES) {
            try {
                XposedHelpers.findAndHookMethod(className, classLoader, "a", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        Object action = param.getResult();
                        if (action == null) {
                            return;
                        }
                        if (applyProfileActionHidden(action, hiddenVisibilityHandlerClass)) {
                            logOnce("archived-profile-hidden",
                                    "archived profile menu action hidden");
                        } else {
                            logOnce("archived-profile-hide-failed",
                                    "profile archived action hook fired but hide field write failed");
                        }
                    }
                });
                log("hook installed: " + className + ".a() profile archived hide");
                hookedAny = true;
            } catch (Throwable ignored) {
                // Try next action provider candidate.
            }
        }
        if (!hookedAny) {
            log("hook unavailable: profile archived action provider class not found");
        }
    }

    private static void hookSearchTrigger(ClassLoader classLoader) {
        try {
            Class<?> zeroStateSearchBoxClass = XposedHelpers.findClass(ZERO_STATE_SEARCH_BOX_CLASS,
                    classLoader);
            XposedHelpers.findAndHookMethod(zeroStateSearchBoxClass, "onFinishInflate",
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            Object searchBox = param.thisObject;
                            if (!(searchBox instanceof View)) {
                                return;
                            }
                            Object inputObj = XposedHelpers.getObjectField(searchBox, "d");
                            if (!(inputObj instanceof View)) {
                                return;
                            }
                            View inputView = (View) inputObj;
                            Object marker = XposedHelpers.getAdditionalInstanceField(inputView,
                                    "cmreborn_search_trigger_watcher");
                            if (marker != null) {
                                return;
                            }
                            TextWatcher watcher = new TextWatcher() {
                                @Override
                                public void beforeTextChanged(CharSequence s, int start, int count,
                                        int after) {
                                }

                                @Override
                                public void onTextChanged(CharSequence s, int start, int before,
                                        int count) {
                                    if (s == null || !SEARCH_TRIGGER.contentEquals(s)) {
                                        return;
                                    }
                                    handleSearchTriggerFromSearchBox(searchBox);
                                }

                                @Override
                                public void afterTextChanged(Editable s) {
                                }
                            };
                            XposedHelpers.callMethod(inputObj, "addTextChangedListener", watcher);
                            XposedHelpers.setAdditionalInstanceField(inputView,
                                    "cmreborn_search_trigger_watcher", watcher);
                        }
                    });
            log("hook installed: ZeroStateSearchBox.onFinishInflate trigger watcher");
        } catch (Throwable t) {
            logThrowable("hook failed: ZeroStateSearchBox.onFinishInflate trigger watcher", t);
        }
    }

    private static void handleSearchTriggerFromSearchBox(Object searchBox) {
        long now = SystemClock.elapsedRealtime();
        if (now - lastTriggerAtMs < TRIGGER_THROTTLE_MS) {
            return;
        }
        lastTriggerAtMs = now;

        if (!(searchBox instanceof View)) {
            log("search trigger detected, but search box was not a View");
            return;
        }

        Object account = null;
        try {
            Object searchPeer = XposedHelpers.getObjectField(searchBox, "f");
            if (searchPeer != null) {
                account = readFieldIfPresent(searchPeer, "n");
                if (account == null) {
                    account = readFieldIfPresent(searchPeer, "m");
                }
            }
        } catch (Throwable t) {
            logThrowable("search trigger account lookup failed", t);
        }

        log("search trigger detected");
        openArchivedActivity(((View) searchBox).getContext(), account);
    }

    private static void hookSearchHomeCleanup(ClassLoader classLoader) {
        boolean hookedAny = false;
        for (String className : SEARCH_HOME_FRAGMENT_CLASS_CANDIDATES) {
            try {
                XposedHelpers.findAndHookMethod(className, classLoader, "M",
                        LayoutInflater.class, ViewGroup.class, Bundle.class,
                        new XC_MethodHook() {
                            @Override
                            protected void afterHookedMethod(MethodHookParam param) {
                                Object result = param.getResult();
                                if (!(result instanceof View)) {
                                    return;
                                }

                                View root = (View) result;
                                boolean changed = false;
                                changed |= hideChildByName(root, "zero_state_search_home_group");
                                changed |= hideChildByName(root, "zero_state_content_groups");
                                changed |= hideChildByName(root,
                                        "zero_state_content_groups_holder");
                                changed |= hideChildByName(root,
                                        "zero_state_content_grid_layout");
                                if (changed) {
                                    logOnce("search-home-suggestions-hidden",
                                            "search home suggestions hidden");
                                }
                            }
                });
                log("hook installed: " + className + ".M(...) search home cleanup");
                hookedAny = true;
                break;
            } catch (Throwable ignored) {
                // Try next search fragment candidate.
            }
        }
        if (!hookedAny) {
            log("hook unavailable: search home cleanup class not found");
        }
    }

    private static void hookSearchCategorySuggestions(ClassLoader classLoader) {
        final Object emptyResultFuture = buildImmediateFuture(classLoader,
                Collections.emptyList());
        if (emptyResultFuture == null) {
            log("hook unavailable: could not build immediate future for empty search categories");
            return;
        }
        boolean hookedAny = false;
        for (String className : SEARCH_CATEGORY_PROVIDER_CLASS_CANDIDATES) {
            try {
                XposedHelpers.findAndHookMethod(className, classLoader, "b",
                        new XC_MethodHook() {
                            @Override
                            protected void beforeHookedMethod(MethodHookParam param) {
                                param.setResult(emptyResultFuture);
                                logOnce("search-category-suggestions-suppressed",
                                        "search category suggestions suppressed");
                            }
                        });
                log("hook installed: " + className + ".b() empty category list");
                hookedAny = true;
            } catch (Throwable ignored) {
                // Try next category provider candidate.
            }
        }
        if (!hookedAny) {
            log("hook unavailable: category suggestions provider class not found");
        }
    }

    private static Object filterArchivedConversationViewData(Object viewData, ClassLoader classLoader) {
        if (viewData == null || classLoader == null) {
            return viewData;
        }
        try {
            Object starred = XposedHelpers.callMethod(viewData, "c");
            Object conversationList = XposedHelpers.callMethod(viewData, "a");
            Object noMatchingParts = XposedHelpers.callMethod(viewData, "d");
            Object semantic = XposedHelpers.callMethod(viewData, "b");

            int beforeStarred = collectionSize(starred);
            int beforeConversation = collectionSize(conversationList);
            int beforeSemantic = collectionSize(semantic);
            int beforeNoMatching = collectionSize(noMatchingParts);

            FilterListResult starredFiltered = filterArchivedSearchViewItems(starred,
                    "search-view-starred", classLoader);
            FilterListResult conversationFiltered = filterArchivedSearchViewItems(conversationList,
                    "search-view-conversation-list", classLoader);
            FilterListResult semanticFiltered = filterArchivedSearchViewItems(semantic,
                    "search-view-semantic", classLoader);
            FilterSetResult noMatchingFiltered = filterArchivedConversationIdSet(noMatchingParts,
                    "search-view-no-matching-parts", classLoader);

            int removed = starredFiltered.removed + conversationFiltered.removed
                    + semanticFiltered.removed + noMatchingFiltered.removed;
            if (ENABLE_DEBUG_LOGS) {
                log("search view-data counts before="
                        + "starred:" + beforeStarred
                        + " conversations:" + beforeConversation
                        + " semantic:" + beforeSemantic
                        + " noMatch:" + beforeNoMatching
                        + " removed:" + removed);
            }
            if (removed <= 0) {
                return viewData;
            }

            Object rebuilt = rebuildConversationViewData(viewData,
                    starredFiltered.filtered, conversationFiltered.filtered,
                    noMatchingFiltered.filtered, semanticFiltered.filtered, classLoader);
            if (rebuilt == viewData) {
                log("search conversation view-data rebuild unavailable; preserving original result");
                return viewData;
            }
            log("search conversation view-data archive filter applied; removed=" + removed);
            return rebuilt;
        } catch (Throwable t) {
            logThrowable("search conversation view-data rebuild failed", t);
            return viewData;
        }
    }

    private static FilterListResult filterArchivedSearchViewItems(Object maybeList, String source,
            ClassLoader classLoader) {
        if (!(maybeList instanceof java.util.Collection)) {
            return new FilterListResult(maybeList, 0);
        }
        java.util.Collection<?> original = (java.util.Collection<?>) maybeList;
        if (original.isEmpty()) {
            return new FilterListResult(original, 0);
        }
        java.util.ArrayList<Object> kept = new java.util.ArrayList<>(original.size());
        int removed = 0;
        for (Object item : original) {
            int inlineStatus = conversationArchiveStatusFromSearchViewItem(item);
            Object conversationId = extractConversationIdTypeFromSearchViewItem(item);
            int status = ARCHIVE_STATUS_UNKNOWN;
            String decisionSource = "inline";
            if (conversationId != null) {
                status = conversationArchiveStatusFromGlobalLookup(conversationId);
                decisionSource = "global";
                if (status == ARCHIVE_STATUS_UNKNOWN) {
                    status = inlineStatus;
                    decisionSource = "inline-fallback";
                }
            } else {
                status = inlineStatus;
            }
            if (status == ARCHIVE_STATUS_ARCHIVED || status == ARCHIVE_STATUS_KEEP_ARCHIVED) {
                removed++;
                if (ENABLE_DEBUG_LOGS) {
                    log(source + " removed archived search item; via=" + decisionSource
                            + "; conversationId=" + conversationIdLong(conversationId)
                            + "; status=" + status);
                }
                continue;
            }
            kept.add(item);
        }
        if (removed <= 0) {
            return new FilterListResult(original, 0);
        }
        Object rebuilt = rebuildImmutableListLike(maybeList, kept, classLoader);
        if (ENABLE_DEBUG_LOGS) {
            log(source + " filtered archived entries removed=" + removed);
        }
        return new FilterListResult(rebuilt, removed);
    }

    private static FilterSetResult filterArchivedConversationIdSet(Object maybeSet, String source,
            ClassLoader classLoader) {
        if (!(maybeSet instanceof java.util.Collection)) {
            return new FilterSetResult(maybeSet, 0);
        }
        java.util.Collection<?> original = (java.util.Collection<?>) maybeSet;
        if (original.isEmpty()) {
            return new FilterSetResult(original, 0);
        }
        java.util.ArrayList<Object> kept = new java.util.ArrayList<>(original.size());
        int removed = 0;
        for (Object conversationId : original) {
            int status = conversationArchiveStatusFromGlobalLookup(conversationId);
            if (status == ARCHIVE_STATUS_ARCHIVED || status == ARCHIVE_STATUS_KEEP_ARCHIVED) {
                removed++;
                continue;
            }
            kept.add(conversationId);
        }
        if (removed <= 0) {
            return new FilterSetResult(original, 0);
        }
        Object rebuilt = rebuildImmutableSetLike(maybeSet, kept, classLoader);
        if (ENABLE_DEBUG_LOGS) {
            log(source + " filtered archived entries removed=" + removed);
        }
        return new FilterSetResult(rebuilt, removed);
    }

    private static int conversationArchiveStatusFromSearchViewItem(Object item) {
        if (item == null) {
            return ARCHIVE_STATUS_UNKNOWN;
        }
        try {
            Object conversationData = XposedHelpers.callMethod(item, "l");
            if (conversationData == null) {
                return ARCHIVE_STATUS_UNKNOWN;
            }
            Object archiveStatus = XposedHelpers.callMethod(conversationData, "B");
            if (archiveStatus == null) {
                return ARCHIVE_STATUS_UNKNOWN;
            }
            return archiveStatusCodeFromObject(archiveStatus);
        } catch (Throwable ignored) {
            return ARCHIVE_STATUS_UNKNOWN;
        }
    }

    private static Object extractConversationIdTypeFromSearchViewItem(Object item) {
        if (item == null) {
            return null;
        }
        try {
            Object conversationId = XposedHelpers.callMethod(item, "y");
            if (looksLikeConversationIdType(conversationId)) {
                return conversationId;
            }
        } catch (Throwable ignored) {
            // Try other known item shapes.
        }
        try {
            Object rowModel = XposedHelpers.callMethod(item, "l");
            if (rowModel != null) {
                Object conversationId = XposedHelpers.callMethod(rowModel, "y");
                if (looksLikeConversationIdType(conversationId)) {
                    return conversationId;
                }
            }
        } catch (Throwable ignored) {
            // Try other known item shapes.
        }
        try {
            Object cardModel = XposedHelpers.callMethod(item, "g");
            if (cardModel != null) {
                Object conversationId = XposedHelpers.callMethod(cardModel, "c");
                if (looksLikeConversationIdType(conversationId)) {
                    return conversationId;
                }
            }
        } catch (Throwable ignored) {
            // No supported conversation-id accessor on this item type.
        }
        return null;
    }

    private static Object rebuildConversationViewData(Object originalViewData, Object starred,
            Object conversationList, Object noMatchingParts, Object semantic, ClassLoader classLoader) {
        Object[] args = {starred, conversationList, noMatchingParts, semantic};
        Object rebuilt = tryInstantiateWithMatchingConstructor(
                originalViewData != null ? originalViewData.getClass() : null, args);
        if (rebuilt != null) {
            return rebuilt;
        }
        for (String className : SEARCH_VIEW_DATA_CONCRETE_CLASS_CANDIDATES) {
            try {
                Class<?> candidate = XposedHelpers.findClass(className, classLoader);
                rebuilt = tryInstantiateWithMatchingConstructor(candidate, args);
                if (rebuilt != null) {
                    return rebuilt;
                }
            } catch (Throwable ignored) {
                // Try next concrete candidate.
            }
        }
        return originalViewData;
    }

    private static Object tryInstantiateWithMatchingConstructor(Class<?> targetClass, Object[] args) {
        if (targetClass == null || args == null) {
            return null;
        }
        try {
            for (Constructor<?> constructor : targetClass.getDeclaredConstructors()) {
                Class<?>[] parameterTypes = constructor.getParameterTypes();
                if (parameterTypes.length != args.length) {
                    continue;
                }
                if (!constructorArgsMatch(parameterTypes, args)) {
                    continue;
                }
                constructor.setAccessible(true);
                return constructor.newInstance(args);
            }
        } catch (Throwable ignored) {
            // Fall back to other concrete class candidates.
        }
        return null;
    }

    private static boolean constructorArgsMatch(Class<?>[] parameterTypes, Object[] args) {
        if (parameterTypes == null || args == null || parameterTypes.length != args.length) {
            return false;
        }
        for (int i = 0; i < parameterTypes.length; i++) {
            if (!isParameterCompatible(parameterTypes[i], args[i])) {
                return false;
            }
        }
        return true;
    }

    private static boolean isParameterCompatible(Class<?> parameterType, Object arg) {
        if (parameterType == null) {
            return false;
        }
        if (arg == null) {
            return !parameterType.isPrimitive();
        }
        if (parameterType.isInstance(arg)) {
            return true;
        }
        if (!parameterType.isPrimitive()) {
            return false;
        }
        return (parameterType == boolean.class && arg instanceof Boolean)
                || (parameterType == byte.class && arg instanceof Byte)
                || (parameterType == short.class && arg instanceof Short)
                || (parameterType == int.class && arg instanceof Integer)
                || (parameterType == long.class && arg instanceof Long)
                || (parameterType == float.class && arg instanceof Float)
                || (parameterType == double.class && arg instanceof Double)
                || (parameterType == char.class && arg instanceof Character);
    }

    private static Object rebuildImmutableListLike(Object originalCollection,
            java.util.Collection<Object> kept, ClassLoader classLoader) {
        Object rebuilt = rebuildImmutableCollectionLike(originalCollection, kept, classLoader,
                IMMUTABLE_LIST_CLASS_CANDIDATES);
        return rebuilt != null ? rebuilt : new java.util.ArrayList<>(kept);
    }

    private static Object rebuildImmutableSetLike(Object originalCollection,
            java.util.Collection<Object> kept, ClassLoader classLoader) {
        Object rebuilt = rebuildImmutableCollectionLike(originalCollection, kept, classLoader,
                IMMUTABLE_SET_CLASS_CANDIDATES);
        return rebuilt != null ? rebuilt : new java.util.LinkedHashSet<>(kept);
    }

    private static Object rebuildImmutableCollectionLike(Object originalCollection,
            java.util.Collection<Object> kept, ClassLoader classLoader, String[] classCandidates) {
        if (kept == null) {
            return null;
        }
        if (originalCollection != null) {
            Object rebuilt = callCollectionFactoryMethods(originalCollection.getClass(), kept);
            if (rebuilt != null) {
                return rebuilt;
            }
        }
        if (classLoader == null || classCandidates == null) {
            return null;
        }
        for (String className : classCandidates) {
            try {
                Class<?> candidate = XposedHelpers.findClass(className, classLoader);
                Object rebuilt = callCollectionFactoryMethods(candidate, kept);
                if (rebuilt != null) {
                    return rebuilt;
                }
            } catch (Throwable ignored) {
                // Try next candidate collection class.
            }
        }
        return null;
    }

    private static Object callCollectionFactoryMethods(Class<?> factoryClass,
            java.util.Collection<Object> values) {
        if (factoryClass == null || values == null) {
            return null;
        }
        String[] factoryMethodNames = {"n", "o", "j"};
        for (String methodName : factoryMethodNames) {
            try {
                return XposedHelpers.callStaticMethod(factoryClass, methodName, values);
            } catch (Throwable ignored) {
                // Try next factory method.
            }
        }
        Object[] array = values.toArray();
        try {
            return XposedHelpers.callStaticMethod(factoryClass, "i", array, array.length);
        } catch (Throwable ignored) {
            // Continue fallback.
        }
        try {
            return XposedHelpers.callStaticMethod(factoryClass, "p", array);
        } catch (Throwable ignored) {
            // No compatible collection factory in this class.
        }
        return null;
    }

    private static final class FilterListResult {
        final Object filtered;
        final int removed;

        FilterListResult(Object filtered, int removed) {
            this.filtered = filtered;
            this.removed = removed;
        }
    }

    private static final class FilterSetResult {
        final Object filtered;
        final int removed;

        FilterSetResult(Object filtered, int removed) {
            this.filtered = filtered;
            this.removed = removed;
        }
    }

    @SuppressWarnings("unchecked")
    private static void hookSearchResultAdapters(ClassLoader classLoader) {
        boolean hookedAny = false;
        hookedAny |= hookSearchResultAdapterMethods(classLoader,
                SEARCH_CONVERSATION_RESULTS_ADAPTER_CLASS_CANDIDATES,
                "search conversation list adapter");
        hookedAny |= hookSearchResultAdapterMethods(classLoader,
                SEARCH_STARRED_RESULTS_ADAPTER_CLASS_CANDIDATES,
                "search starred-message adapter");
        if (!hookedAny) {
            log("hook unavailable: search result adapters not found");
        }
    }

    private static boolean hookSearchResultAdapterMethods(ClassLoader classLoader,
            String[] adapterClassCandidates, String sourceLabel) {
        if (adapterClassCandidates == null || adapterClassCandidates.length == 0) {
            return false;
        }
        boolean hooked = false;
        for (String adapterClassName : adapterClassCandidates) {
            boolean hookedThisAdapter = false;
            for (String viewDataClassName : SEARCH_VIEW_DATA_ABSTRACT_CLASS_CANDIDATES) {
                try {
                    Class<?> viewDataClass = XposedHelpers.findClass(viewDataClassName, classLoader);
                    XposedHelpers.findAndHookMethod(adapterClassName, classLoader, "F",
                            viewDataClass, buildSearchResultAdapterHook(sourceLabel));
                    log("hook installed: " + adapterClassName + ".F(" + viewDataClassName + ") "
                            + sourceLabel + " archived-result UI filter");
                    hookedThisAdapter = true;
                    hooked = true;
                    break;
                } catch (Throwable ignored) {
                    // Try next view-data class candidate.
                }
            }
            if (!hookedThisAdapter) {
                log("hook unavailable: " + adapterClassName
                        + ".F(<search-view-data>) not found");
            }
        }
        return hooked;
    }

    private static XC_MethodHook buildSearchResultAdapterHook(final String sourceLabel) {
        return new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                try {
                    Object filtered = filterArchivedConversationViewData(
                            param.args[0],
                            param.thisObject.getClass().getClassLoader());
                    if (filtered != param.args[0]) {
                        param.args[0] = filtered;
                        if (ENABLE_DEBUG_LOGS) {
                            log(sourceLabel + " conversation filter applied: "
                                    + param.thisObject.getClass().getSimpleName() + "."
                                    + param.method.getName());
                        }
                    }
                } catch (Throwable t) {
                    logThrowable(sourceLabel + " archive filter failed", t);
                }
            }
        };
    }

    @SuppressWarnings("unchecked")
    private static void hookAttachmentResultAdapters(ClassLoader classLoader) {
        boolean hooked = false;
        for (String candidate : ATTACHMENT_RESULT_ADAPTER_METHOD_CANDIDATES) {
            if (TextUtils.isEmpty(candidate)) {
                continue;
            }
            int separator = candidate.indexOf('#');
            if (separator <= 0 || separator >= candidate.length() - 1) {
                continue;
            }
            String className = candidate.substring(0, separator);
            String methodName = candidate.substring(separator + 1);
            hooked |= hookAttachmentResultAdapterMethod(classLoader, className, methodName);
        }
        if (!hooked) {
            log("hook unavailable: attachment result adapters not found");
        }
    }

    private static boolean hookAttachmentResultAdapterMethod(ClassLoader classLoader,
            String className, String methodName) {
        final XC_MethodHook hook = buildAttachmentResultAdapterHook(className);
        final Class<?> targetClass;
        try {
            targetClass = XposedHelpers.findClass(className, classLoader);
        } catch (Throwable ignored) {
            return false;
        }
        try {
            XposedHelpers.findAndHookMethod(targetClass, methodName, java.util.List.class,
                    hook);
            log("hook installed: " + className + "." + methodName + "(List) attachment archive filter");
            return true;
        } catch (Throwable ignored) {
            if (ENABLE_DEBUG_LOGS) {
                log("hook unavailable: " + className + "." + methodName + "(List)");
            }
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    private static XC_MethodHook buildAttachmentResultAdapterHook(final String sourceLabel) {
        return new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                Object arg = param.args[0];
                if (!(arg instanceof java.util.List)) {
                    return;
                }
                java.util.List<?> filtered = filterArchivedAttachmentItems(
                        (java.util.List<?>) arg, sourceLabel + "." + param.method.getName());
                param.args[0] = filtered;
            }
        };
    }

    private static void hookSearchSuggestionContactFilter(ClassLoader classLoader) {
        boolean hookedAny = false;
        for (String className : SEARCH_SUGGESTION_FILTER_CLASS_CANDIDATES) {
            try {
                XposedHelpers.findAndHookMethod(className, classLoader, "performFiltering",
                        CharSequence.class, buildSearchSuggestionFilterHook());
                log("hook installed: " + className
                        + ".performFiltering(CharSequence) archived-suggestion filter");
                hookedAny = true;
                break;
            } catch (Throwable ignored) {
                // Try next filter class candidate.
            }
        }
        if (!hookedAny) {
            log("hook unavailable: search suggestion filter class not found");
        }
    }

    private static XC_MethodHook buildSearchSuggestionFilterHook() {
        return new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                try {
                    Object filterResults = param.getResult();
                    if (filterResults == null) {
                        return;
                    }
                    Object valuesObj = XposedHelpers.getObjectField(filterResults, "values");
                    if (!(valuesObj instanceof java.util.List)) {
                        return;
                    }
                    @SuppressWarnings("unchecked")
                    java.util.List<Object> values = (java.util.List<Object>) valuesObj;
                    java.util.List<?> filtered = filterArchivedSearchFilterDataItems(values,
                            param.thisObject.getClass().getClassLoader(),
                            "search suggestions");
                    if (filtered == values) {
                        return;
                    }
                    XposedHelpers.setObjectField(filterResults, "values", filtered);
                    XposedHelpers.setIntField(filterResults, "count", filtered.size());
                    param.setResult(filterResults);
                    if (ENABLE_DEBUG_LOGS) {
                        log("search suggestions filtered archived participant entries");
                    }
                } catch (Throwable t) {
                    logThrowable("search suggestions archive filter failed", t);
                }
            }
        };
    }

    private static void hookSearchContactResultsFilter(ClassLoader classLoader) {
        boolean hookedAny = false;
        for (String candidate : SEARCH_CONTACT_RESULTS_ADAPTER_METHOD_CANDIDATES) {
            if (TextUtils.isEmpty(candidate)) {
                continue;
            }
            int separator = candidate.indexOf('#');
            if (separator <= 0 || separator >= candidate.length() - 1) {
                continue;
            }
            String className = candidate.substring(0, separator);
            String methodName = candidate.substring(separator + 1);
            try {
                XposedHelpers.findAndHookMethod(className, classLoader, methodName,
                        java.util.List.class,
                        buildSearchContactResultsHook());
                log("hook installed: " + className + "." + methodName
                        + "(List) archived-contact-result filter");
                hookedAny = true;
                break;
            } catch (Throwable ignored) {
                // Try next contact results adapter/method candidate.
            }
        }
        if (!hookedAny) {
            log("hook unavailable: search contact-results adapter not found");
        }
    }

    private static XC_MethodHook buildSearchContactResultsHook() {
        return new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                try {
                    Object arg = param.args[0];
                    if (!(arg instanceof java.util.List)) {
                        return;
                    }
                    java.util.List<?> filtered = filterArchivedContactRows(
                            (java.util.List<?>) arg,
                            param.thisObject.getClass().getClassLoader(),
                            "search contact results");
                    if (filtered != arg) {
                        param.args[0] = filtered;
                    }
                } catch (Throwable t) {
                    logThrowable("search contact-results archive filter failed", t);
                }
            }
        };
    }

    private static void hookSearchContactTapBehavior(ClassLoader classLoader) {
        boolean hookedAny = false;
        for (String className : SEARCH_CONTACT_TAP_HANDLER_CLASS_CANDIDATES) {
            try {
                XposedHelpers.findAndHookMethod(className, classLoader, "c", Object.class,
                        Object.class, buildSearchContactTapHook());
                log("hook installed: " + className + ".c(Object,Object) contact tap open/block policy");
                hookedAny = true;
                break;
            } catch (Throwable ignored) {
                // Try next contact-tap handler candidate.
            }
        }
        if (!hookedAny) {
            log("hook unavailable: search contact-tap handler class not found");
        }
    }

    private static XC_MethodHook buildSearchContactTapHook() {
        return new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                Object[] args = param.args;
                if (args == null || args.length < 2) {
                    return;
                }
                Object recipient = args[0];
                Object isParticipantObj = args[1];
                if (!(isParticipantObj instanceof Boolean)
                        || !((Boolean) isParticipantObj).booleanValue()) {
                    return;
                }
                if (recipient == null) {
                    return;
                }

                String participantLookupKey = null;
                try {
                    Object twp = XposedHelpers.callMethod(recipient, "f");
                    Object keyObj = twp != null ? readFieldIfPresent(twp, "k") : null;
                    participantLookupKey = keyObj instanceof String ? (String) keyObj : null;
                } catch (Throwable t) {
                    logThrowable("search contact tap lookup-key read failed", t);
                }

                boolean archivedOnly = isParticipantLookupKeyArchivedOnly(
                        participantLookupKey,
                        param.thisObject.getClass().getClassLoader(), null);
                if (archivedOnly) {
                    log("blocked archived-only contact search tap; participant_lookup_key="
                            + participantLookupKey);
                    param.setResult(null);
                    return;
                }
                param.args[1] = Boolean.FALSE;
                log("rerouted participant contact search tap to direct-open branch; participant_lookup_key="
                        + participantLookupKey);
            }
        };
    }

    private static java.util.List<?> filterArchivedSearchFilterDataItems(
            java.util.List<?> original, ClassLoader classLoader, String source) {
        if (original == null || original.isEmpty()) {
            return original;
        }
        java.util.ArrayList<Object> kept = new java.util.ArrayList<>(original.size());
        java.util.HashMap<String, Boolean> cache = new java.util.HashMap<>();
        int removed = 0;
        for (Object item : original) {
            String participantLookupKey = extractParticipantLookupKeyFromSearchFilterDataItem(item);
            if (!TextUtils.isEmpty(participantLookupKey)
                    && isParticipantLookupKeyArchivedOnly(participantLookupKey, classLoader,
                    cache)) {
                removed++;
                continue;
            }
            kept.add(item);
        }
        if (removed > 0) {
            log(source + " filtered archived participant entries removed=" + removed);
            return kept;
        }
        return original;
    }

    private static java.util.List<?> filterArchivedContactRows(java.util.List<?> original,
            ClassLoader classLoader, String source) {
        if (original == null || original.isEmpty()) {
            return original;
        }
        java.util.ArrayList<Object> kept = new java.util.ArrayList<>(original.size());
        java.util.HashMap<String, Boolean> cache = new java.util.HashMap<>();
        int removed = 0;
        for (Object row : original) {
            String participantLookupKey = extractParticipantLookupKeyFromContactRow(row);
            String destination = extractDestinationFromContactRow(row);
            long contactId = extractContactIdFromContactRow(row);
            if (isContactIdentityArchivedOnly(participantLookupKey, destination, contactId,
                    classLoader, cache)) {
                removed++;
                continue;
            }
            kept.add(row);
        }
        if (removed > 0) {
            log(source + " filtered archived contacts removed=" + removed);
            return kept;
        }
        return original;
    }

    private static String extractParticipantLookupKeyFromSearchFilterDataItem(Object item) {
        if (item == null) {
            return null;
        }
        try {
            Object searchFilter = XposedHelpers.callMethod(item, "a");
            String participantLookupKey =
                    extractParticipantLookupKeyFromParticipantSearchFilter(searchFilter);
            if (!TextUtils.isEmpty(participantLookupKey)) {
                return participantLookupKey;
            }
        } catch (Throwable ignored) {
            // Fall through.
        }
        try {
            Object searchFilter = XposedHelpers.getObjectField(item, "b");
            String participantLookupKey =
                    extractParticipantLookupKeyFromParticipantSearchFilter(searchFilter);
            if (!TextUtils.isEmpty(participantLookupKey)) {
                return participantLookupKey;
            }
        } catch (Throwable ignored) {
            // No supported participant lookup-key field on this item type.
        }
        return null;
    }

    private static String extractParticipantLookupKeyFromParticipantSearchFilter(
            Object searchFilter) {
        if (searchFilter == null) {
            return null;
        }
        String className = searchFilter.getClass().getName();
        if (!className.contains("ParticipantSearchFilter")) {
            return null;
        }
        try {
            Object keyObj = XposedHelpers.getObjectField(searchFilter, "a");
            if (keyObj instanceof String && !TextUtils.isEmpty((String) keyObj)) {
                return (String) keyObj;
            }
        } catch (Throwable ignored) {
            // Fallback to reflective string-field scan.
        }
        try {
            for (Field field : searchFilter.getClass().getDeclaredFields()) {
                if (field.getType() != String.class) {
                    continue;
                }
                field.setAccessible(true);
                Object value = field.get(searchFilter);
                if (value instanceof String && !TextUtils.isEmpty((String) value)) {
                    return (String) value;
                }
            }
        } catch (Throwable ignored) {
            // Keep null when schema is unsupported.
        }
        return null;
    }

    private static String extractParticipantLookupKeyFromContactRow(Object row) {
        if (row == null) {
            return null;
        }
        try {
            Object keyObj = XposedHelpers.getObjectField(row, "k");
            if (keyObj instanceof String && !TextUtils.isEmpty((String) keyObj)) {
                return (String) keyObj;
            }
        } catch (Throwable ignored) {
            // Fall back to best-effort reflective scan.
        }
        try {
            String displayName = safeStringField(row, "b");
            String destination = safeStringField(row, "c");
            for (Field field : row.getClass().getDeclaredFields()) {
                if (field.getType() != String.class) {
                    continue;
                }
                field.setAccessible(true);
                Object value = field.get(row);
                if (!(value instanceof String)) {
                    continue;
                }
                String candidate = (String) value;
                if (TextUtils.isEmpty(candidate)) {
                    continue;
                }
                if (candidate.equals(displayName) || candidate.equals(destination)) {
                    continue;
                }
                // Lookup keys usually contain separators and are not plain phone/address text.
                if (candidate.indexOf('/') >= 0 || candidate.indexOf(':') >= 0
                        || candidate.indexOf(';') >= 0 || candidate.length() > 18) {
                    return candidate;
                }
            }
        } catch (Throwable ignored) {
            // Keep null if fallback scan fails.
        }
        return null;
    }

    private static String extractDestinationFromContactRow(Object row) {
        if (row == null) {
            return null;
        }
        try {
            Object destinationObj = XposedHelpers.getObjectField(row, "c");
            return destinationObj instanceof String ? (String) destinationObj : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static long extractContactIdFromContactRow(Object row) {
        if (row == null) {
            return -1L;
        }
        try {
            return XposedHelpers.getLongField(row, "f");
        } catch (Throwable ignored) {
            // Fall back to any plausible positive long field.
        }
        try {
            for (Field field : row.getClass().getDeclaredFields()) {
                Class<?> type = field.getType();
                if (type != long.class && type != Long.class) {
                    continue;
                }
                field.setAccessible(true);
                Object value = field.get(row);
                long candidate = value instanceof Number ? ((Number) value).longValue() : -1L;
                if (candidate > 0L) {
                    return candidate;
                }
            }
        } catch (Throwable ignored) {
            // Keep unknown when row schema is unsupported.
        }
        return -1L;
    }

    private static String safeStringField(Object target, String fieldName) {
        if (target == null || TextUtils.isEmpty(fieldName)) {
            return null;
        }
        try {
            Object value = XposedHelpers.getObjectField(target, fieldName);
            return value instanceof String ? (String) value : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static boolean isParticipantLookupKeyArchivedOnly(String participantLookupKey,
            ClassLoader classLoader, java.util.Map<String, Boolean> cache) {
        if (TextUtils.isEmpty(participantLookupKey)) {
            return false;
        }
        if (cache != null) {
            Boolean cached = cache.get(participantLookupKey);
            if (cached != null) {
                return cached.booleanValue();
            }
        }
        boolean archivedOnly = isParticipantLookupKeyArchivedOnly(participantLookupKey,
                classLoader);
        if (cache != null) {
            cache.put(participantLookupKey, Boolean.valueOf(archivedOnly));
        }
        return archivedOnly;
    }

    private static boolean isContactIdentityArchivedOnly(String participantLookupKey,
            String destination, long contactId, ClassLoader classLoader,
            java.util.Map<String, Boolean> cache) {
        if (classLoader == null) {
            return false;
        }
        String cacheKey = String.valueOf(participantLookupKey) + "|"
                + String.valueOf(destination) + "|" + contactId;
        if (cache != null) {
            Boolean cached = cache.get(cacheKey);
            if (cached != null) {
                return cached.booleanValue();
            }
        }
        boolean archivedOnly = isContactIdentityArchivedOnly(participantLookupKey, destination,
                contactId, classLoader);
        if (cache != null) {
            cache.put(cacheKey, Boolean.valueOf(archivedOnly));
        }
        return archivedOnly;
    }

    private static boolean isContactIdentityArchivedOnly(String participantLookupKey,
            String destination, long contactId, ClassLoader classLoader) {
        Cursor cursor = null;
        SQLiteDatabase database = null;
        try {
            java.util.ArrayList<String> clauses = new java.util.ArrayList<>();
            java.util.ArrayList<String> args = new java.util.ArrayList<>();

            if (!TextUtils.isEmpty(participantLookupKey)) {
                clauses.add("(participant_lookup_key = ? OR normalized_participant_lookup_key = ?)");
                args.add(participantLookupKey);
                args.add(participantLookupKey);
            }
            if (contactId > 0L) {
                String contactIdArg = String.valueOf(contactId);
                clauses.add("(participant_contact_id = ? OR normalized_participant_contact_id = ?)");
                args.add(contactIdArg);
                args.add(contactIdArg);
            }
            if (!TextUtils.isEmpty(destination)) {
                java.util.LinkedHashSet<String> variants = destinationVariants(destination);
                for (String value : variants) {
                    clauses.add("(participant_normalized_destination = ? "
                            + "OR participant_comparable_destination = ? "
                            + "OR participant_display_destination = ? "
                            + "OR normalized_participant_display_destination = ?)");
                    args.add(value);
                    args.add(value);
                    args.add(value);
                    args.add(value);
                }
            }

            if (clauses.isEmpty()) {
                return false;
            }

            String sql = "SELECT archive_status FROM conversations WHERE "
                    + TextUtils.join(" OR ", clauses);
            database = openMessagesDatabase(false);
            if (database == null) {
                return false;
            }
            cursor = database.rawQuery(sql, args.toArray(new String[0]));
            if (cursor == null) {
                return false;
            }

            boolean hasAny = false;
            boolean hasArchived = false;
            boolean hasVisible = false;
            while (cursor.moveToNext()) {
                hasAny = true;
                int status = cursor.getInt(0);
                if (status == ARCHIVE_STATUS_ARCHIVED || status == ARCHIVE_STATUS_KEEP_ARCHIVED) {
                    hasArchived = true;
                } else {
                    hasVisible = true;
                    break;
                }
            }
            return hasAny && hasArchived && !hasVisible;
        } catch (Throwable t) {
            logThrowable("contact identity archive query failed", t);
            return false;
        } finally {
            if (cursor != null) {
                try {
                    cursor.close();
                } catch (Throwable ignored) {
                    // Ignore cursor close failures during hook execution.
                }
            }
            closeQuietly(database);
        }
    }

    private static java.util.LinkedHashSet<String> destinationVariants(String destination) {
        java.util.LinkedHashSet<String> variants = new java.util.LinkedHashSet<>();
        if (TextUtils.isEmpty(destination)) {
            return variants;
        }
        variants.add(destination);
        String trimmed = destination.trim();
        if (!trimmed.equals(destination)) {
            variants.add(trimmed);
        }
        String comparable = trimmed.replaceAll("[^0-9+]", "");
        if (!TextUtils.isEmpty(comparable)) {
            variants.add(comparable);
            if (comparable.startsWith("+") && comparable.length() > 1) {
                variants.add(comparable.substring(1));
            }
        }
        return variants;
    }

    private static boolean isParticipantLookupKeyArchivedOnly(String participantLookupKey,
            ClassLoader classLoader) {
        if (TextUtils.isEmpty(participantLookupKey) || classLoader == null) {
            return false;
        }
        Cursor cursor = null;
        SQLiteDatabase database = null;
        try {
            database = openMessagesDatabase(false);
            if (database == null) {
                return false;
            }
            cursor = database.rawQuery(
                    "SELECT archive_status FROM conversations "
                            + "WHERE participant_lookup_key = ? "
                            + "OR normalized_participant_lookup_key = ?",
                    new String[]{participantLookupKey, participantLookupKey});
            if (cursor == null) {
                return false;
            }
            boolean hasAny = false;
            boolean hasArchived = false;
            boolean hasVisible = false;
            while (cursor.moveToNext()) {
                hasAny = true;
                int status = cursor.getInt(0);
                if (status == ARCHIVE_STATUS_ARCHIVED || status == ARCHIVE_STATUS_KEEP_ARCHIVED) {
                    hasArchived = true;
                } else {
                    hasVisible = true;
                    break;
                }
            }
            return hasAny && hasArchived && !hasVisible;
        } catch (Throwable t) {
            logThrowable("participant lookup-key archive query failed", t);
            return false;
        } finally {
            if (cursor != null) {
                try {
                    cursor.close();
                } catch (Throwable ignored) {
                    // Ignore cursor close failures during hook execution.
                }
            }
            closeQuietly(database);
        }
    }

    private static java.util.List<?> filterArchivedAttachmentItems(java.util.List<?> original,
            String source) {
        if (original == null || original.isEmpty()) {
            return original;
        }
        try {
            java.util.ArrayList<Object> filtered = new java.util.ArrayList<>(original.size());
            java.util.HashMap<String, Integer> archiveStatusCache = new java.util.HashMap<>();
            int removed = 0;
            for (Object item : original) {
                Object conversationId = extractConversationIdTypeFromAttachmentItem(item);
                int status = ARCHIVE_STATUS_UNKNOWN;
                if (conversationId != null) {
                    String cacheKey = String.valueOf(conversationId);
                    Integer cachedStatus = archiveStatusCache.get(cacheKey);
                    if (cachedStatus != null) {
                        status = cachedStatus.intValue();
                    } else {
                        status = conversationArchiveStatusFromGlobalLookup(conversationId);
                        archiveStatusCache.put(cacheKey, Integer.valueOf(status));
                    }
                }
                if (status == ARCHIVE_STATUS_ARCHIVED || status == ARCHIVE_STATUS_KEEP_ARCHIVED) {
                    removed++;
                    continue;
                }
                filtered.add(item);
            }
            if (removed > 0) {
                log("attachment results filtered in " + source + "; removed=" + removed);
            }
            return filtered;
        } catch (Throwable t) {
            logThrowable("attachment result filter failed in " + source, t);
            return original;
        }
    }

    private static Object extractConversationIdTypeFromAttachmentItem(Object item) {
        if (item == null) {
            return null;
        }
        try {
            // MediaSearchResult -> k(): ConversationIdType
            Object maybeConversationId = XposedHelpers.callMethod(item, "k");
            if (looksLikeConversationIdType(maybeConversationId)) {
                return maybeConversationId;
            }
            // Link card presenter item -> k(): UrlSearchResult -> m(): ConversationIdType
            if (maybeConversationId != null) {
                Object maybeLinkConversationId = XposedHelpers.callMethod(maybeConversationId, "m");
                if (looksLikeConversationIdType(maybeLinkConversationId)) {
                    return maybeLinkConversationId;
                }
            }
        } catch (Throwable ignored) {
            // Try location card path next.
        }
        try {
            // Location card presenter (dpwo) -> g(): ckww -> c(): ConversationIdType
            Object locationModel = XposedHelpers.callMethod(item, "g");
            if (locationModel == null) {
                return null;
            }
            Object maybeConversationId = XposedHelpers.callMethod(locationModel, "c");
            return looksLikeConversationIdType(maybeConversationId) ? maybeConversationId : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static boolean looksLikeConversationIdType(Object value) {
        if (value == null) {
            return false;
        }
        String className = value.getClass().getName();
        return "com.google.android.apps.messaging.shared.datamodel.data.datatypes.ConversationIdType"
                .equals(className);
    }

    private static void hookArchiveNotificationAllowPolicy(ClassLoader classLoader) {
        try {
            final Class<?> conversationIdTypeClass = XposedHelpers.findClass(
                    "com.google.android.apps.messaging.shared.datamodel.data.datatypes.ConversationIdType",
                    classLoader);
            final Class<?> archiveStatusClass = findArchiveStatusEnumClass(classLoader);
            final Class<?> eventReasonClass = findClassAny(classLoader,
                    ARCHIVE_REASON_CLASS_CANDIDATES);
            final Class<?> conversationIdListClass = findClassAny(classLoader,
                    ARCHIVE_ID_LIST_CLASS_CANDIDATES);
            if (archiveStatusClass == null || eventReasonClass == null
                    || conversationIdListClass == null) {
                log("hook unavailable: archive notification toggle dependent classes not found");
                return;
            }

            boolean hookedAny = false;
            for (String archiveApiClass : ARCHIVE_API_IMPL_CLASS_CANDIDATES) {
                try {
                    XposedHelpers.findAndHookMethod(archiveApiClass, classLoader, "b",
                            conversationIdTypeClass, archiveStatusClass, eventReasonClass,
                            boolean.class, new XC_MethodHook() {
                                @Override
                                protected void afterHookedMethod(MethodHookParam param) {
                                    try {
                                        Object result = param.getResult();
                                        if (!(result instanceof Boolean) || !((Boolean) result)) {
                                            return;
                                        }
                                        Object conversationId = param.args[0];
                                        Object status = param.args[1];
                                        applyArchiveNotificationPolicy(param.thisObject,
                                                java.util.Collections.singletonList(conversationId),
                                                status, param.thisObject.getClass().getSimpleName()
                                                        + ".b");
                                    } catch (Throwable t) {
                                        logThrowable("archive notification toggle hook failed: b", t);
                                    }
                                }
                            });
                    log("hook installed: " + archiveApiClass
                            + ".b(...) archive notification toggle policy");
                    hookedAny = true;
                } catch (Throwable ignored) {
                    // Try next class candidate.
                }

                try {
                    XposedHelpers.findAndHookMethod(archiveApiClass, classLoader, "c",
                            conversationIdListClass, archiveStatusClass, eventReasonClass,
                            new XC_MethodHook() {
                                @Override
                                protected void afterHookedMethod(MethodHookParam param) {
                                    try {
                                        Object status = param.args[1];
                                        Object result = param.getResult();
                                        java.util.Collection<?> conversations =
                                                toCollection(result, param.args[0]);
                                        if (conversations == null || conversations.isEmpty()) {
                                            return;
                                        }
                                        applyArchiveNotificationPolicy(param.thisObject,
                                                conversations, status,
                                                param.thisObject.getClass().getSimpleName() + ".c");
                                    } catch (Throwable t) {
                                        logThrowable("archive notification toggle hook failed: c", t);
                                    }
                                }
                            });
                    log("hook installed: " + archiveApiClass
                            + ".c(...) archive notification toggle policy");
                    hookedAny = true;
                } catch (Throwable ignored) {
                    // Try next class candidate.
                }
            }
            if (!hookedAny) {
                log("hook unavailable: archive notification toggle api class not found");
            }
        } catch (Throwable t) {
            logThrowable("hook failed: archive notification toggle policy resolution", t);
        }
    }

    private static java.util.Collection<?> toCollection(Object primary, Object fallback) {
        if (primary instanceof java.util.Collection) {
            return (java.util.Collection<?>) primary;
        }
        if (fallback instanceof java.util.Collection) {
            return (java.util.Collection<?>) fallback;
        }
        return null;
    }

    private static void applyArchiveNotificationPolicy(Object archiveApiImpl,
            java.util.Collection<?> conversationIds, Object archiveStatus, String source) {
        if (archiveApiImpl == null || conversationIds == null || conversationIds.isEmpty()
                || archiveStatus == null) {
            return;
        }
        int status = resolveArchiveStatusCode(archiveStatus);
        if (status == ARCHIVE_STATUS_UNKNOWN) {
            String statusClass = archiveStatus.getClass().getName();
            logOnce("archive-status-unresolved:" + statusClass,
                    "archive notification toggle status unresolved; source=" + source
                            + "; class=" + statusClass);
            return;
        }

        Boolean notificationEnabled;
        if (status == ARCHIVE_STATUS_ARCHIVED || status == ARCHIVE_STATUS_KEEP_ARCHIVED) {
            notificationEnabled = Boolean.FALSE;
        } else if (status == ARCHIVE_STATUS_UNARCHIVED) {
            notificationEnabled = Boolean.TRUE;
        } else {
            return;
        }

        int updated = 0;
        int attempted = 0;
        for (Object conversationId : conversationIds) {
            if (conversationId == null) {
                continue;
            }
            attempted++;
            if (setConversationNotificationEnabled(archiveApiImpl, conversationId,
                    notificationEnabled.booleanValue())) {
                updated++;
            }
        }
        log("archive notification toggle applied in " + source + "; enable="
                + notificationEnabled + "; updated=" + updated + "/" + attempted);
    }

    private static int resolveArchiveStatusCode(Object archiveStatus) {
        if (archiveStatus == null) {
            return ARCHIVE_STATUS_UNKNOWN;
        }
        try {
            return XposedHelpers.getIntField(archiveStatus, "i");
        } catch (Throwable ignored) {
            // Fall through to other schemas.
        }
        try {
            Object value = XposedHelpers.callMethod(archiveStatus, "a");
            if (value instanceof Number) {
                return ((Number) value).intValue();
            }
        } catch (Throwable ignored) {
            // Fall through to enum-name fallback.
        }
        if (archiveStatus instanceof Enum<?>) {
            String name = ((Enum<?>) archiveStatus).name();
            if ("UNARCHIVED".equals(name)) {
                return ARCHIVE_STATUS_UNARCHIVED;
            }
            if ("ARCHIVED".equals(name)) {
                return ARCHIVE_STATUS_ARCHIVED;
            }
            if ("KEEP_ARCHIVED".equals(name)) {
                return ARCHIVE_STATUS_KEEP_ARCHIVED;
            }
            return ((Enum<?>) archiveStatus).ordinal();
        }
        return ARCHIVE_STATUS_UNKNOWN;
    }

    private static boolean setConversationNotificationEnabled(Object archiveApiImpl,
            Object conversationId, boolean enabled) {
        boolean dbUpdated = setConversationNotificationEnabledInDatabase(archiveApiImpl,
                conversationId, enabled);
        boolean channelUpdated = setConversationNotificationChannelEnabled(archiveApiImpl,
                conversationId, enabled);
        if (ENABLE_DEBUG_LOGS) {
            log("set conversation notification policy complete; enabled=" + enabled
                    + "; dbUpdated=" + dbUpdated + "; channelUpdated=" + channelUpdated
                    + "; conversation=" + String.valueOf(conversationId));
        }
        return dbUpdated || channelUpdated;
    }

    private static boolean setConversationNotificationEnabledInDatabase(Object archiveApiImpl,
            Object conversationId, boolean enabled) {
        SQLiteDatabase database = null;
        try {
            long conversationIdLong = conversationIdLong(conversationId);
            if (conversationIdLong <= 0L) {
                return false;
            }
            database = openMessagesDatabase(true);
            if (database == null) {
                throw new IllegalStateException("messages database unavailable");
            }
            ContentValues values = new ContentValues();
            values.put("notification_enabled", Boolean.valueOf(enabled));
            int updated = database.update("conversations", values, "_id = ?",
                    new String[]{String.valueOf(conversationIdLong)});
            return updated > 0;
        } catch (Throwable t) {
            logThrowable("set conversation notification_enabled failed", t);
            return false;
        } finally {
            closeQuietly(database);
        }
    }

    private static SQLiteDatabase openMessagesDatabase(boolean writable) {
        Context context = getRuntimeContext();
        if (context == null) {
            return null;
        }
        java.util.LinkedHashSet<String> names = new java.util.LinkedHashSet<>();
        names.add("bugle_db");
        try {
            String[] listed = context.databaseList();
            if (listed != null) {
                for (String name : listed) {
                    if (TextUtils.isEmpty(name)
                            || name.endsWith("-journal")
                            || name.endsWith("-wal")
                            || name.endsWith("-shm")) {
                        continue;
                    }
                    if (name.contains("bugle") || name.contains("message")) {
                        names.add(name);
                    }
                }
            }
        } catch (Throwable ignored) {
            // Fall back to known database name.
        }
        for (String name : names) {
            try {
                File databasePath = context.getDatabasePath(name);
                if (databasePath == null || !databasePath.exists()) {
                    continue;
                }
                int flags = writable
                        ? SQLiteDatabase.OPEN_READWRITE
                        : SQLiteDatabase.OPEN_READONLY;
                return SQLiteDatabase.openDatabase(databasePath.getAbsolutePath(), null, flags);
            } catch (Throwable ignored) {
                // Try next candidate database name.
            }
        }
        return null;
    }

    private static void closeQuietly(SQLiteDatabase database) {
        if (database == null) {
            return;
        }
        try {
            database.close();
        } catch (Throwable ignored) {
            // Ignore close failures for best-effort DB access.
        }
    }

    private static boolean setConversationNotificationChannelEnabled(Object archiveApiImpl,
            Object conversationId, boolean enabled) {
        if (conversationId == null) {
            return false;
        }
        try {
            Context context = resolveArchiveApiContext(archiveApiImpl);
            if (context == null) {
                context = getRuntimeContext();
            }
            if (context == null) {
                log("set conversation channel toggle skipped: context unavailable");
                return false;
            }

            NotificationManager notificationManager = (NotificationManager) context
                    .getSystemService(Context.NOTIFICATION_SERVICE);
            if (notificationManager == null) {
                log("set conversation channel toggle skipped: notification manager unavailable");
                return false;
            }

            LinkedHashSet<String> conversationKeys =
                    resolveConversationChannelKeys(archiveApiImpl, conversationId);
            if (conversationKeys.isEmpty()) {
                log("set conversation channel toggle skipped: no conversation key candidates");
                return false;
            }

            String parentChannelId = resolveImParentChannelId(context);
            NotificationChannel existingChannel = findConversationChannel(notificationManager,
                    parentChannelId, conversationKeys);

            String conversationKey = null;
            if (existingChannel != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                conversationKey = existingChannel.getConversationId();
            }
            if (TextUtils.isEmpty(conversationKey)) {
                conversationKey = conversationKeys.iterator().next();
            }
            if (TextUtils.isEmpty(conversationKey)) {
                return false;
            }

            String channelId = existingChannel != null ? existingChannel.getId() : null;
            if (TextUtils.isEmpty(channelId)) {
                if (!TextUtils.isEmpty(parentChannelId)) {
                    channelId = parentChannelId + " : " + conversationKey;
                } else {
                    channelId = conversationKey;
                }
            }
            String fixedChannelId = !TextUtils.isEmpty(parentChannelId)
                    ? parentChannelId + " : " + conversationKey
                    : channelId;

            int targetImportance;
            if (enabled) {
                Integer previousImportance = CHANNEL_IMPORTANCE_CACHE.remove(fixedChannelId);
                if (previousImportance == null && !TextUtils.equals(fixedChannelId, channelId)) {
                    previousImportance = CHANNEL_IMPORTANCE_CACHE.remove(channelId);
                }
                targetImportance = previousImportance != null
                        ? sanitizeChannelImportance(previousImportance.intValue())
                        : NotificationManager.IMPORTANCE_DEFAULT;
            } else {
                if (existingChannel != null) {
                    int existingImportance = existingChannel.getImportance();
                    if (existingImportance > NotificationManager.IMPORTANCE_NONE) {
                        cacheChannelImportance(fixedChannelId, existingImportance);
                        cacheChannelImportance(channelId, existingImportance);
                    }
                }
                targetImportance = NotificationManager.IMPORTANCE_NONE;
            }

            NotificationChannel rebuiltChannel =
                    buildConversationChannel(context, existingChannel, channelId, conversationKey,
                            parentChannelId, targetImportance);

            // Force a deterministic state transition by recreating the channel.
            notificationManager.deleteNotificationChannel(channelId);
            notificationManager.createNotificationChannel(rebuiltChannel);
            NotificationChannel committed = notificationManager.getNotificationChannel(channelId);

            int committedImportance = committed != null
                    ? committed.getImportance()
                    : NotificationManager.IMPORTANCE_UNSPECIFIED;
            boolean success = enabled
                    ? committedImportance > NotificationManager.IMPORTANCE_NONE
                    : committedImportance == NotificationManager.IMPORTANCE_NONE;

            if (enabled && !success && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
                    && !TextUtils.isEmpty(parentChannelId)) {
                String migratedChannelId =
                        fixedChannelId + "#cmr_on#" + SystemClock.elapsedRealtime();
                LinkedHashSet<String> staleChannelIds = collectConversationChannelIds(
                        notificationManager, parentChannelId, conversationKeys);
                staleChannelIds.add(channelId);
                staleChannelIds.add(fixedChannelId);
                for (String staleChannelId : staleChannelIds) {
                    if (TextUtils.isEmpty(staleChannelId)) {
                        continue;
                    }
                    try {
                        notificationManager.deleteNotificationChannel(staleChannelId);
                    } catch (Throwable ignored) {
                        // Best effort cleanup of stale channels.
                    }
                }
                NotificationChannel migratedChannel = buildConversationChannel(context,
                        committed != null ? committed : existingChannel, migratedChannelId,
                        conversationKey, parentChannelId, targetImportance);
                notificationManager.createNotificationChannel(migratedChannel);
                NotificationChannel resolved =
                        notificationManager.getNotificationChannel(parentChannelId, conversationKey);
                if (resolved == null) {
                    resolved = notificationManager.getNotificationChannel(migratedChannelId);
                }
                if (resolved != null) {
                    channelId = resolved.getId();
                    committedImportance = resolved.getImportance();
                    success = committedImportance > NotificationManager.IMPORTANCE_NONE;
                    if (success) {
                        CHANNEL_IMPORTANCE_CACHE.remove(fixedChannelId);
                        CHANNEL_IMPORTANCE_CACHE.remove(channelId);
                    }
                } else {
                    success = false;
                }
                log("set conversation channel toggle fallback migration; conversationKey="
                        + conversationKey + "; removedChannelIds=" + staleChannelIds
                        + "; migratedChannelId=" + migratedChannelId + "; resolvedChannelId="
                        + (resolved != null ? resolved.getId() : "null")
                        + "; resolvedImportance=" + committedImportance + "; success=" + success);
            } else if (!enabled && !success && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
                    && !TextUtils.isEmpty(parentChannelId)) {
                String migratedChannelId =
                        fixedChannelId + "#cmr_off#" + SystemClock.elapsedRealtime();
                LinkedHashSet<String> staleChannelIds = collectConversationChannelIds(
                        notificationManager, parentChannelId, conversationKeys);
                staleChannelIds.add(channelId);
                staleChannelIds.add(fixedChannelId);
                for (String staleChannelId : staleChannelIds) {
                    if (TextUtils.isEmpty(staleChannelId)) {
                        continue;
                    }
                    try {
                        notificationManager.deleteNotificationChannel(staleChannelId);
                    } catch (Throwable ignored) {
                        // Best effort cleanup of stale channels.
                    }
                }
                NotificationChannel migratedChannel = buildConversationChannel(context,
                        committed != null ? committed : existingChannel, migratedChannelId,
                        conversationKey, parentChannelId, targetImportance);
                notificationManager.createNotificationChannel(migratedChannel);
                NotificationChannel resolved =
                        notificationManager.getNotificationChannel(parentChannelId, conversationKey);
                if (resolved == null) {
                    resolved = notificationManager.getNotificationChannel(migratedChannelId);
                }
                if (resolved != null) {
                    channelId = resolved.getId();
                    committedImportance = resolved.getImportance();
                    success = committedImportance == NotificationManager.IMPORTANCE_NONE;
                } else {
                    success = false;
                }
                log("set conversation channel toggle disable fallback; conversationKey="
                        + conversationKey + "; removedChannelIds=" + staleChannelIds
                        + "; migratedChannelId=" + migratedChannelId + "; resolvedChannelId="
                        + (resolved != null ? resolved.getId() : "null")
                        + "; resolvedImportance=" + committedImportance + "; success=" + success);
            }

            log("set conversation channel toggle applied; enabled=" + enabled
                    + "; conversationKey=" + conversationKey
                    + "; channelId=" + channelId
                    + "; parentChannelId=" + parentChannelId
                    + "; targetImportance=" + targetImportance
                    + "; committedImportance=" + committedImportance
                    + "; success=" + success);
            return success;
        } catch (Throwable t) {
            logThrowable("set conversation notification channel failed", t);
            return false;
        }
    }

    private static Context resolveArchiveApiContext(Object archiveApiImpl) {
        if (archiveApiImpl == null) {
            return null;
        }
        try {
            Object conversationStoreProvider = XposedHelpers.getObjectField(archiveApiImpl, "d");
            Object conversationStore = XposedHelpers.callMethod(conversationStoreProvider, "a");
            Object context = readFieldIfPresent(conversationStore, "a");
            if (context instanceof Context) {
                return (Context) context;
            }
        } catch (Throwable ignored) {
            // Fall through to runtime context fallback.
        }
        return null;
    }

    private static LinkedHashSet<String> resolveConversationChannelKeys(Object archiveApiImpl,
            Object conversationId) {
        LinkedHashSet<String> keys = new LinkedHashSet<>();
        if (conversationId == null) {
            return keys;
        }
        try {
            Object shortcutUtil = archiveApiImpl != null
                    ? readFieldIfPresent(archiveApiImpl, "k")
                    : null;
            if (shortcutUtil != null) {
                Object resolved = XposedHelpers.callMethod(shortcutUtil, "b", conversationId);
                if (resolved instanceof String && !TextUtils.isEmpty((String) resolved)) {
                    keys.add((String) resolved);
                }
            }
        } catch (Throwable ignored) {
            // Continue with fallback candidates.
        }
        try {
            Object value = XposedHelpers.callMethod(conversationId, "a");
            if (value instanceof String && !TextUtils.isEmpty((String) value)) {
                keys.add((String) value);
            } else if (value instanceof Number) {
                keys.add(String.valueOf(((Number) value).longValue()));
            }
        } catch (Throwable ignored) {
            // Continue with additional fallbacks.
        }
        try {
            Object fieldValue = readFieldIfPresent(conversationId, "a");
            if (fieldValue instanceof String && !TextUtils.isEmpty((String) fieldValue)) {
                keys.add((String) fieldValue);
            } else if (fieldValue instanceof Number) {
                keys.add(String.valueOf(((Number) fieldValue).longValue()));
            }
        } catch (Throwable ignored) {
            // Continue to toString fallback.
        }
        String fallback = String.valueOf(conversationId);
        if (!TextUtils.isEmpty(fallback)) {
            keys.add(fallback);
        }
        return keys;
    }

    private static String resolveImParentChannelId(Context context) {
        if (context == null) {
            return null;
        }
        int id = resourceId(context, "string", "im_notification_default_channel_id");
        if (id != 0) {
            try {
                String value = context.getString(id);
                if (!TextUtils.isEmpty(value)) {
                    return value;
                }
            } catch (Throwable ignored) {
                // Try next candidate id.
            }
        }
        id = resourceId(context, "string", "im_notification_default_channel_id_v2");
        if (id != 0) {
            try {
                String value = context.getString(id);
                if (!TextUtils.isEmpty(value)) {
                    return value;
                }
            } catch (Throwable ignored) {
                // No more candidates.
            }
        }
        return null;
    }

    private static NotificationChannel findConversationChannel(NotificationManager notificationManager,
            String parentChannelId, Set<String> conversationKeys) {
        if (notificationManager == null || conversationKeys == null || conversationKeys.isEmpty()) {
            return null;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !TextUtils.isEmpty(parentChannelId)) {
            for (String key : conversationKeys) {
                try {
                    NotificationChannel channel =
                            notificationManager.getNotificationChannel(parentChannelId, key);
                    if (channel != null) {
                        return channel;
                    }
                } catch (Throwable ignored) {
                    // Fall through to list scan.
                }
            }
        }
        List<NotificationChannel> channels = notificationManager.getNotificationChannels();
        if (channels != null) {
            for (NotificationChannel channel : channels) {
                if (channel == null) {
                    continue;
                }
                String channelId = channel.getId();
                if (!TextUtils.isEmpty(channelId) && channelIdMatchesConversationKey(channelId,
                        conversationKeys)) {
                    return channel;
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    String conversationId = channel.getConversationId();
                    if (!TextUtils.isEmpty(conversationId) && conversationKeys.contains(
                            conversationId)) {
                        if (TextUtils.isEmpty(parentChannelId)
                                || TextUtils.equals(parentChannelId, channel.getParentChannelId())) {
                            return channel;
                        }
                    }
                }
            }
        }
        if (!TextUtils.isEmpty(parentChannelId)) {
            for (String key : conversationKeys) {
                String channelId = parentChannelId + " : " + key;
                try {
                    NotificationChannel channel = notificationManager.getNotificationChannel(
                            channelId);
                    if (channel != null) {
                        return channel;
                    }
                } catch (Throwable ignored) {
                    // Ignore and continue scanning candidates.
                }
            }
        }
        return null;
    }

    private static LinkedHashSet<String> collectConversationChannelIds(
            NotificationManager notificationManager, String parentChannelId,
            Set<String> conversationKeys) {
        LinkedHashSet<String> ids = new LinkedHashSet<>();
        if (notificationManager == null || conversationKeys == null || conversationKeys.isEmpty()) {
            return ids;
        }
        if (!TextUtils.isEmpty(parentChannelId) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            for (String key : conversationKeys) {
                if (TextUtils.isEmpty(key)) {
                    continue;
                }
                try {
                    NotificationChannel channel = notificationManager.getNotificationChannel(
                            parentChannelId, key);
                    if (channel != null && !TextUtils.isEmpty(channel.getId())) {
                        ids.add(channel.getId());
                    }
                } catch (Throwable ignored) {
                    // Try other key candidates.
                }
            }
        }
        try {
            List<NotificationChannel> channels = notificationManager.getNotificationChannels();
            if (channels == null) {
                return ids;
            }
            for (NotificationChannel channel : channels) {
                if (channel == null) {
                    continue;
                }
                String channelId = channel.getId();
                boolean matches = channelIdMatchesConversationKey(channelId, conversationKeys);
                if (!matches && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    String conversationId = channel.getConversationId();
                    matches = !TextUtils.isEmpty(conversationId)
                            && conversationKeys.contains(conversationId)
                            && (TextUtils.isEmpty(parentChannelId)
                            || TextUtils.equals(parentChannelId, channel.getParentChannelId()));
                }
                if (matches && !TextUtils.isEmpty(channelId)) {
                    ids.add(channelId);
                }
            }
        } catch (Throwable ignored) {
            // Channel enumeration is best effort.
        }
        return ids;
    }

    private static boolean channelIdMatchesConversationKey(String channelId,
            Set<String> conversationKeys) {
        if (TextUtils.isEmpty(channelId) || conversationKeys == null || conversationKeys.isEmpty()) {
            return false;
        }
        for (String key : conversationKeys) {
            if (TextUtils.isEmpty(key)) {
                continue;
            }
            if (TextUtils.equals(channelId, key) || channelId.endsWith(" : " + key)) {
                return true;
            }
        }
        return false;
    }

    private static NotificationChannel buildConversationChannel(Context context,
            NotificationChannel existingChannel, String channelId, String conversationKey,
            String parentChannelId, int importance) {
        String channelName = conversationKey;
        if (existingChannel != null && existingChannel.getName() != null) {
            channelName = String.valueOf(existingChannel.getName());
        }
        NotificationChannel channel = new NotificationChannel(channelId, channelName,
                sanitizeChannelImportance(importance));
        if (existingChannel != null) {
            try {
                channel.setDescription(existingChannel.getDescription());
                channel.setGroup(existingChannel.getGroup());
                channel.enableLights(existingChannel.shouldShowLights());
                channel.setLightColor(existingChannel.getLightColor());
                channel.setVibrationPattern(existingChannel.getVibrationPattern());
                channel.enableVibration(existingChannel.shouldVibrate());
                channel.setShowBadge(existingChannel.canShowBadge());
                channel.setBypassDnd(existingChannel.canBypassDnd());
                channel.setLockscreenVisibility(existingChannel.getLockscreenVisibility());
                if (importance > NotificationManager.IMPORTANCE_NONE) {
                    channel.setSound(existingChannel.getSound(), existingChannel.getAudioAttributes());
                } else {
                    channel.setSound(null, null);
                }
            } catch (Throwable ignored) {
                // Best-effort copy; remaining fields are optional.
            }
        } else if (context != null && !TextUtils.isEmpty(parentChannelId)) {
            try {
                NotificationManager notificationManager = (NotificationManager) context
                        .getSystemService(Context.NOTIFICATION_SERVICE);
                NotificationChannel parent = notificationManager != null
                        ? notificationManager.getNotificationChannel(parentChannelId)
                        : null;
                if (parent != null) {
                    channel.setGroup(parent.getGroup());
                    if (importance > NotificationManager.IMPORTANCE_NONE) {
                        channel.setSound(parent.getSound(), parent.getAudioAttributes());
                    } else {
                        channel.setSound(null, null);
                    }
                    channel.enableVibration(parent.shouldVibrate());
                    channel.setVibrationPattern(parent.getVibrationPattern());
                    channel.enableLights(parent.shouldShowLights());
                    channel.setLightColor(parent.getLightColor());
                    channel.setShowBadge(parent.canShowBadge());
                }
            } catch (Throwable ignored) {
                // Parent sync is optional.
            }
        }
        if (importance == NotificationManager.IMPORTANCE_NONE) {
            channel.enableVibration(false);
            channel.setVibrationPattern(null);
            channel.setSound(null, null);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
                && !TextUtils.isEmpty(parentChannelId)
                && !TextUtils.isEmpty(conversationKey)) {
            channel.setConversationId(parentChannelId, conversationKey);
        }
        return channel;
    }

    private static int sanitizeChannelImportance(int importance) {
        if (importance < NotificationManager.IMPORTANCE_NONE
                || importance > NotificationManager.IMPORTANCE_MAX) {
            return NotificationManager.IMPORTANCE_DEFAULT;
        }
        return importance;
    }

    private static void cacheChannelImportance(String channelId, int importance) {
        if (TextUtils.isEmpty(channelId)) {
            return;
        }
        clearMapIfOversized(CHANNEL_IMPORTANCE_CACHE, MAX_CHANNEL_IMPORTANCE_CACHE_SIZE);
        CHANNEL_IMPORTANCE_CACHE.put(channelId, sanitizeChannelImportance(importance));
    }

    private static int archiveStatusCodeFromObject(Object archiveStatus) {
        if (!looksLikeArchiveStatusEnum(archiveStatus)) {
            return ARCHIVE_STATUS_UNKNOWN;
        }
        try {
            String name = ((Enum<?>) archiveStatus).name();
            if ("UNARCHIVED".equals(name)) {
                return 0;
            }
            if ("ARCHIVED".equals(name)) {
                return ARCHIVE_STATUS_ARCHIVED;
            }
            if ("KEEP_ARCHIVED".equals(name)) {
                return ARCHIVE_STATUS_KEEP_ARCHIVED;
            }
        } catch (Throwable ignored) {
            // Fall back to raw enum value only if name lookup fails.
        }
        try {
            return XposedHelpers.getIntField(archiveStatus, "i");
        } catch (Throwable ignored) {
            // Keep unknown when enum shape is unexpected.
        }
        return ARCHIVE_STATUS_UNKNOWN;
    }

    private static boolean looksLikeArchiveStatusEnum(Object archiveStatus) {
        if (!(archiveStatus instanceof Enum<?>)) {
            return false;
        }
        try {
            Class<?> enumClass = archiveStatus.getClass();
            Object[] constants = enumClass.getEnumConstants();
            if (constants == null || constants.length == 0) {
                return false;
            }
            boolean hasUnarchived = false;
            boolean hasArchived = false;
            boolean hasKeepArchived = false;
            for (Object constant : constants) {
                if (!(constant instanceof Enum<?>)) {
                    continue;
                }
                String name = ((Enum<?>) constant).name();
                if ("UNARCHIVED".equals(name)) {
                    hasUnarchived = true;
                } else if ("ARCHIVED".equals(name)) {
                    hasArchived = true;
                } else if ("KEEP_ARCHIVED".equals(name)) {
                    hasKeepArchived = true;
                }
            }
            return hasUnarchived && hasArchived && hasKeepArchived;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static int conversationArchiveStatusFromGlobalLookup(Object conversationId) {
        if (conversationId == null) {
            return ARCHIVE_STATUS_UNKNOWN;
        }
        Cursor cursor = null;
        SQLiteDatabase database = null;
        try {
            long conversationIdLong = conversationIdLong(conversationId);
            if (conversationIdLong <= 0L) {
                return ARCHIVE_STATUS_UNKNOWN;
            }
            database = openMessagesDatabase(false);
            if (database == null) {
                return ARCHIVE_STATUS_UNKNOWN;
            }
            cursor = database.rawQuery(
                    "SELECT archive_status FROM conversations WHERE _id = ? LIMIT 1",
                    new String[]{String.valueOf(conversationIdLong)});
            if (cursor == null || !cursor.moveToFirst()) {
                return ARCHIVE_STATUS_UNKNOWN;
            }
            return cursor.getInt(0);
        } catch (Throwable t) {
            logOnce("global-archive-status-lookup-unavailable",
                    "global archive status lookup unavailable; default fallbacks active");
            return ARCHIVE_STATUS_UNKNOWN;
        } finally {
            if (cursor != null) {
                try {
                    cursor.close();
                } catch (Throwable ignored) {
                    // Ignore cursor close failures during hook execution.
                }
            }
            closeQuietly(database);
        }
    }

    private static long conversationIdLong(Object conversationId) {
        if (conversationId == null) {
            return -1L;
        }
        try {
            return XposedHelpers.getLongField(conversationId, "a");
        } catch (Throwable ignored) {
            // Fall through to method/string parsing fallback.
        }
        try {
            Object idValue = XposedHelpers.callMethod(conversationId, "a");
            if (idValue instanceof Number) {
                return ((Number) idValue).longValue();
            }
            if (idValue instanceof String) {
                return Long.parseLong((String) idValue);
            }
        } catch (Throwable ignored) {
            // Fall through to toString fallback.
        }
        try {
            return Long.parseLong(String.valueOf(conversationId));
        } catch (Throwable ignored) {
            return -1L;
        }
    }

    private static int collectionSize(Object maybeCollection) {
        if (maybeCollection instanceof java.util.Collection) {
            return ((java.util.Collection<?>) maybeCollection).size();
        }
        return -1;
    }

    private static void hookArchivedSelectionUnarchiveVisibility(ClassLoader classLoader) {
        boolean hookedAny = false;
        for (String className : ARCHIVED_SELECTION_CONTROLLER_CLASS_CANDIDATES) {
            final Class<?> controllerClass;
            try {
                controllerClass = XposedHelpers.findClass(className, classLoader);
            } catch (Throwable ignored) {
                continue;
            }
            if (!ActionMode.Callback.class.isAssignableFrom(controllerClass)) {
                continue;
            }

            boolean hookedClass = false;
            try {
                XposedHelpers.findAndHookMethod(className, classLoader, "b", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        try {
                            if (enforceArchivedSelectionUnarchive(param.thisObject)) {
                                logOnce("archived-unarchive-visible",
                                        "archived selection unarchive action enforced");
                            }
                        } catch (Throwable t) {
                            logThrowable("archived selection unarchive enforce failed: b()", t);
                        }
                    }
                });
                log("hook installed: " + className + ".b() enforce unarchive in Archived folder");
                hookedClass = true;
                hookedAny = true;
            } catch (Throwable ignored) {
                // Method may have changed; continue with other lifecycle hooks.
            }

            try {
                XposedHelpers.findAndHookMethod(className, classLoader, "e", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        try {
                            if (enforceArchivedSelectionUnarchive(param.thisObject)) {
                                logOnce("archived-unarchive-visible",
                                        "archived selection unarchive action enforced");
                            }
                        } catch (Throwable t) {
                            logThrowable("archived selection unarchive enforce failed: e()", t);
                        }
                    }
                });
                log("hook installed: " + className + ".e() enforce unarchive in Archived folder");
                hookedClass = true;
                hookedAny = true;
            } catch (Throwable ignored) {
                // Method may have changed; continue with other lifecycle hooks.
            }

            try {
                XposedHelpers.findAndHookMethod(className, classLoader, "onCreateActionMode",
                        ActionMode.class, Menu.class, new XC_MethodHook() {
                            @Override
                            protected void afterHookedMethod(MethodHookParam param) {
                                try {
                                    if (enforceArchivedSelectionUnarchive(param.thisObject)) {
                                        logOnce("archived-unarchive-visible",
                                                "archived selection unarchive action enforced");
                                    }
                                } catch (Throwable t) {
                                    logThrowable("archived selection unarchive enforce failed: onCreateActionMode",
                                            t);
                                }
                            }
                        });
                log("hook installed: " + className + ".onCreateActionMode(...)");
                hookedClass = true;
                hookedAny = true;
            } catch (Throwable ignored) {
                // Try additional hooks for this class.
            }

            try {
                XposedHelpers.findAndHookMethod(className, classLoader, "onPrepareActionMode",
                        ActionMode.class, Menu.class, new XC_MethodHook() {
                            @Override
                            protected void afterHookedMethod(MethodHookParam param) {
                                try {
                                    if (enforceArchivedSelectionUnarchive(param.thisObject)) {
                                        logOnce("archived-unarchive-visible",
                                                "archived selection unarchive action enforced");
                                    }
                                } catch (Throwable t) {
                                    logThrowable("archived selection unarchive enforce failed: onPrepareActionMode",
                                            t);
                                }
                            }
                        });
                log("hook installed: " + className + ".onPrepareActionMode(...)");
                hookedClass = true;
                hookedAny = true;
            } catch (Throwable ignored) {
                // Try next class candidate.
            }

            if (!hookedClass) {
                log("hook unavailable: " + className
                        + " had no compatible archived-selection methods");
            }
        }
        if (!hookedAny) {
            log("hook unavailable: archived selection controller class not found");
        }
    }

    private static void hookUserUnarchiveActionSignal(ClassLoader classLoader) {
        boolean hookedAny = false;
        for (String className : ARCHIVED_SELECTION_CONTROLLER_CLASS_CANDIDATES) {
            final Class<?> controllerClass;
            try {
                controllerClass = XposedHelpers.findClass(className, classLoader);
            } catch (Throwable ignored) {
                continue;
            }
            if (!ActionMode.Callback.class.isAssignableFrom(controllerClass)) {
                continue;
            }
            try {
                XposedHelpers.findAndHookMethod(className, classLoader, "onActionItemClicked",
                        ActionMode.class, MenuItem.class, new XC_MethodHook() {
                            @Override
                            protected void beforeHookedMethod(MethodHookParam param) {
                                MenuItem menuItem = param.args != null && param.args.length > 1
                                        ? (MenuItem) param.args[1] : null;
                                if (!isUnarchiveActionMenuItem(param.thisObject, menuItem)) {
                                    return;
                                }
                                enterUserUnarchiveAction();
                                param.setObjectExtra("cmreborn_manual_unarchive", Boolean.TRUE);
                                log("manual unarchive action detected");
                            }

                            @Override
                            protected void afterHookedMethod(MethodHookParam param) {
                                if (!Boolean.TRUE.equals(
                                        param.getObjectExtra("cmreborn_manual_unarchive"))) {
                                    return;
                                }
                                exitUserUnarchiveAction();
                            }
                        });
                log("hook installed: " + className
                        + ".onActionItemClicked(...) manual unarchive signal");
                hookedAny = true;
            } catch (Throwable ignored) {
                // Try next class candidate.
            }
        }
        if (!hookedAny) {
            log("hook unavailable: manual unarchive signal hook not found");
        }
    }

    private static boolean enforceArchivedSelectionUnarchive(Object selectionController) {
        if (selectionController == null || !isArchivedFolderSelectionController(selectionController)) {
            return false;
        }

        int selectedCount = selectedConversationCount(selectionController);
        if (selectedCount <= 0) {
            return false;
        }

        Menu menu = null;
        Object menuObj = readFieldIfPresent(selectionController, "n");
        if (!(menuObj instanceof Menu)) {
            menuObj = readFieldIfPresent(selectionController, "o");
        }
        if (menuObj instanceof Menu) {
            menu = (Menu) menuObj;
        }

        MenuItem unarchiveItem = null;
        if (menu != null) {
            int unarchiveId = resourceIdFromMenuContext(menu, "action_unarchive");
            if (unarchiveId == 0) {
                unarchiveId = INSPECTED_ACTION_UNARCHIVE_ID;
            }
            MenuItem fallbackUnarchive = menu.findItem(unarchiveId);
            if (fallbackUnarchive != null) {
                unarchiveItem = fallbackUnarchive;
            }
        }
        if (unarchiveItem == null) {
            Object unarchiveObj = readFieldIfPresent(selectionController, "q");
            if (!(unarchiveObj instanceof MenuItem)) {
                unarchiveObj = readFieldIfPresent(selectionController, "p");
            }
            if (unarchiveObj instanceof MenuItem) {
                unarchiveItem = (MenuItem) unarchiveObj;
            }
        }

        if (unarchiveItem != null) {
            unarchiveItem.setVisible(true);
            unarchiveItem.setEnabled(true);
        }

        MenuItem archiveItem = null;
        if (menu != null) {
            int archiveId = resourceIdFromMenuContext(menu, "action_archive");
            if (archiveId != 0) {
                archiveItem = menu.findItem(archiveId);
            }
        }
        if (archiveItem == null) {
            Object archiveObj = readFieldIfPresent(selectionController, "p");
            if (!(archiveObj instanceof MenuItem)) {
                archiveObj = readFieldIfPresent(selectionController, "o");
            }
            if (archiveObj instanceof MenuItem) {
                archiveItem = (MenuItem) archiveObj;
            }
        }

        if (archiveItem != null) {
            archiveItem.setVisible(false);
            archiveItem.setEnabled(false);
        }

        return unarchiveItem != null;
    }

    private static boolean isArchivedFolderSelectionController(Object selectionController) {
        Object folderEnum = readFieldIfPresent(selectionController, "d");
        if (folderEnum == null) {
            folderEnum = readFieldIfPresent(selectionController, "c");
        }
        if (folderEnum == null) {
            return false;
        }
        try {
            Object name = XposedHelpers.callMethod(folderEnum, "name");
            return ARCHIVED_FOLDER_ENUM_NAME.equals(String.valueOf(name));
        } catch (Throwable ignored) {
            return ARCHIVED_FOLDER_ENUM_NAME.equals(String.valueOf(folderEnum));
        }
    }

    private static int selectedConversationCount(Object selectionController) {
        Object selectedMap = readFieldIfPresent(selectionController, "a");
        if (selectedMap == null) {
            return 0;
        }
        try {
            return XposedHelpers.getIntField(selectedMap, "d");
        } catch (Throwable ignored) {
            // Fall through to generic size checks.
        }
        if (selectedMap instanceof java.util.Map) {
            return ((java.util.Map<?, ?>) selectedMap).size();
        }
        try {
            Object maybeSize = XposedHelpers.callMethod(selectedMap, "size");
            if (maybeSize instanceof Number) {
                return ((Number) maybeSize).intValue();
            }
        } catch (Throwable ignored) {
            // No-op.
        }
        return 0;
    }

    private static boolean isUnarchiveActionMenuItem(Object selectionController, MenuItem menuItem) {
        if (menuItem == null) {
            return false;
        }
        int itemId = menuItem.getItemId();
        if (itemId == INSPECTED_ACTION_UNARCHIVE_ID) {
            return true;
        }

        Context context = null;
        Object contextField = readFieldIfPresent(selectionController, "g");
        if (contextField instanceof Context) {
            context = (Context) contextField;
        }
        if (context == null) {
            contextField = readFieldIfPresent(selectionController, "f");
        }
        if (contextField instanceof Context) {
            context = (Context) contextField;
        }
        if (context == null) {
            View actionView = menuItem.getActionView();
            if (actionView != null) {
                context = actionView.getContext();
            }
        }
        if (context == null) {
            return false;
        }
        int unarchiveId = resourceId(context, "id", "action_unarchive");
        return unarchiveId != 0 && itemId == unarchiveId;
    }

    private static void enterUserUnarchiveAction() {
        Integer depth = USER_UNARCHIVE_ACTION_DEPTH.get();
        USER_UNARCHIVE_ACTION_DEPTH.set(depth == null ? 1 : depth + 1);
    }

    private static void exitUserUnarchiveAction() {
        Integer depth = USER_UNARCHIVE_ACTION_DEPTH.get();
        if (depth == null || depth <= 1) {
            USER_UNARCHIVE_ACTION_DEPTH.remove();
        } else {
            USER_UNARCHIVE_ACTION_DEPTH.set(depth - 1);
        }
    }

    private static boolean isUserUnarchiveActionActive() {
        Integer depth = USER_UNARCHIVE_ACTION_DEPTH.get();
        return depth != null && depth > 0;
    }

    private static void hookArchivedBackToInbox(ClassLoader classLoader) {
        try {
            XposedHelpers.findAndHookMethod(ARCHIVED_ACTIVITY, classLoader, "onBackPressed",
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            if (!archiveBackToInboxPendingFromTrigger) {
                                return;
                            }
                            archiveBackToInboxPendingFromTrigger = false;
                            Activity archivedActivity = (Activity) param.thisObject;
                            try {
                                Intent intent = new Intent();
                                intent.setClassName(TARGET_PACKAGE, MAIN_ACTIVITY);
                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                                        | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                                archivedActivity.startActivity(intent);
                                archivedActivity.finish();
                                log("archive back rerouted to inbox");
                                param.setResult(null);
                            } catch (Throwable t) {
                                logThrowable("archive back reroute failed", t);
                            }
                        }
                    });
            log("hook installed: ArchivedActivity.onBackPressed");
        } catch (Throwable t) {
            logThrowable("hook failed: ArchivedActivity.onBackPressed", t);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void hookArchivePreserve(ClassLoader classLoader) {
        try {
            final Class<?> archiveStatusClass = findArchiveStatusEnumClass(classLoader);
            if (archiveStatusClass == null) {
                log("hook unavailable: archive preserve status enum class not found");
                return;
            }
            final Object unarchived = Enum.valueOf((Class<Enum>) archiveStatusClass, "UNARCHIVED");
            final Object archived = Enum.valueOf((Class<Enum>) archiveStatusClass, "ARCHIVED");
            final Object keepArchived = Enum.valueOf((Class<Enum>) archiveStatusClass,
                    "KEEP_ARCHIVED");

            boolean hookedAny = false;
            final Class<?> conversationIdTypeClass = XposedHelpers.findClass(
                    "com.google.android.apps.messaging.shared.datamodel.data.datatypes.ConversationIdType",
                    classLoader);
            final Class<?> archiveReasonClass = findClassAny(classLoader,
                    ARCHIVE_REASON_CLASS_CANDIDATES);

            if (archiveReasonClass != null) {
                for (String archiveApiClass : ARCHIVE_API_IMPL_CLASS_CANDIDATES) {
                    try {
                        XposedHelpers.findAndHookMethod(archiveApiClass, classLoader, "a",
                                conversationIdTypeClass, archiveStatusClass, archiveReasonClass,
                                new XC_MethodHook() {
                                    @Override
                                    protected void beforeHookedMethod(MethodHookParam param) {
                                        maybeKeepArchivedFromArchiveApi(param, 0, 1, 2,
                                                unarchived, keepArchived,
                                                param.thisObject.getClass().getSimpleName() + ".a");
                                    }
                                });
                        log("hook installed: " + archiveApiClass + ".a(...) archive preserve");
                        hookedAny = true;
                    } catch (Throwable ignored) {
                        // Try next signature/class candidate.
                    }
                    try {
                        XposedHelpers.findAndHookMethod(archiveApiClass, classLoader, "b",
                                conversationIdTypeClass, archiveStatusClass, archiveReasonClass,
                                boolean.class, new XC_MethodHook() {
                                    @Override
                                    protected void beforeHookedMethod(MethodHookParam param) {
                                        maybeKeepArchivedFromArchiveApi(param, 0, 1, 2,
                                                unarchived, keepArchived,
                                                param.thisObject.getClass().getSimpleName() + ".b");
                                    }
                                });
                        log("hook installed: " + archiveApiClass + ".b(...) archive preserve");
                        hookedAny = true;
                    } catch (Throwable ignored) {
                        // Try next signature/class candidate.
                    }
                    try {
                        boolean hookedListSignature = false;
                        for (String idListClassName : ARCHIVE_ID_LIST_CLASS_CANDIDATES) {
                            Class<?> idListClass = XposedHelpers.findClass(idListClassName,
                                    classLoader);
                            XposedHelpers.findAndHookMethod(archiveApiClass, classLoader, "c",
                                    idListClass, archiveStatusClass, archiveReasonClass,
                                    new XC_MethodHook() {
                                        @Override
                                        protected void beforeHookedMethod(MethodHookParam param) {
                                            maybeKeepArchivedFromArchiveApi(param, 0, 1, 2,
                                                    unarchived, keepArchived,
                                                    param.thisObject.getClass().getSimpleName()
                                                            + ".c");
                                        }
                                    });
                            log("hook installed: " + archiveApiClass + ".c(" + idListClassName
                                    + ",...) archive preserve");
                            hookedAny = true;
                            hookedListSignature = true;
                            break;
                        }
                        if (!hookedListSignature) {
                            // No compatible list class for this archive API class.
                        }
                    } catch (Throwable ignored) {
                        // Try next signature/class candidate.
                    }
                }
            }

            final Class<?> messageIdTypeClass = XposedHelpers.findClass(
                    "com.google.android.apps.messaging.shared.datamodel.data.datatypes.MessageIdType",
                    classLoader);
            final Class<?> selfIdentityIdClass = XposedHelpers.findClass(
                    "com.google.android.apps.messaging.shared.api.messaging.selfidentity.SelfIdentityId",
                    classLoader);
            for (String metadataOpsClass : CONVERSATION_METADATA_OPS_CLASS_CANDIDATES) {
                for (String updateMethodName : new String[]{"j", "i"}) {
                    try {
                        XposedHelpers.findAndHookMethod(metadataOpsClass, classLoader,
                                updateMethodName,
                                conversationIdTypeClass,
                                messageIdTypeClass,
                                Long.class,
                                archiveStatusClass,
                                String.class,
                                boolean.class,
                                long.class,
                                Integer.class,
                                selfIdentityIdClass,
                                boolean.class,
                                new XC_MethodHook() {
                                    @Override
                                    protected void beforeHookedMethod(MethodHookParam param) {
                                        maybeKeepArchived(param, 3, unarchived, archived,
                                                keepArchived, param.thisObject.getClass()
                                                        .getSimpleName() + "."
                                                        + param.method.getName());
                                    }
                                });
                        log("hook installed: " + metadataOpsClass + "." + updateMethodName
                                + "(...) archive preserve");
                        hookedAny = true;
                        break;
                    } catch (Throwable ignored) {
                        // Try the other metadata update method name or next class candidate.
                    }
                }

                try {
                    XposedHelpers.findAndHookMethod(metadataOpsClass, classLoader, "b",
                            conversationIdTypeClass,
                            boolean.class,
                            archiveStatusClass,
                            new XC_MethodHook() {
                                @Override
                                protected void beforeHookedMethod(MethodHookParam param) {
                                    maybeKeepArchived(param, 2, unarchived, archived,
                                            keepArchived, param.thisObject.getClass()
                                                    .getSimpleName() + ".b");
                                }
                            });
                    log("hook installed: " + metadataOpsClass + ".b(...) archive preserve");
                    hookedAny = true;
                } catch (Throwable ignored) {
                    // Try next class candidate.
                }
            }
            if (!hookedAny) {
                log("hook unavailable: archive preserve metadata class not found");
            }
        } catch (Throwable t) {
            logThrowable("hook failed: archive preserve", t);
        }
    }

    private static Class<?> findArchiveStatusEnumClass(ClassLoader classLoader) {
        if (classLoader == null) {
            return null;
        }
        for (String className : ARCHIVE_STATUS_ENUM_CLASS_CANDIDATES) {
            try {
                Class<?> candidate = XposedHelpers.findClass(className, classLoader);
                if (!candidate.isEnum()) {
                    continue;
                }
                Object[] constants = candidate.getEnumConstants();
                if (constants == null || constants.length == 0) {
                    continue;
                }
                boolean hasUnarchived = false;
                boolean hasArchived = false;
                boolean hasKeepArchived = false;
                for (Object constant : constants) {
                    if (!(constant instanceof Enum<?>)) {
                        continue;
                    }
                    String name = ((Enum<?>) constant).name();
                    if ("UNARCHIVED".equals(name)) {
                        hasUnarchived = true;
                    } else if ("ARCHIVED".equals(name)) {
                        hasArchived = true;
                    } else if ("KEEP_ARCHIVED".equals(name)) {
                        hasKeepArchived = true;
                    }
                }
                if (hasUnarchived && hasArchived && hasKeepArchived) {
                    return candidate;
                }
            } catch (Throwable ignored) {
                // Try next class candidate.
            }
        }
        return null;
    }

    private static void maybeKeepArchivedFromArchiveApi(XC_MethodHook.MethodHookParam param,
            int conversationArgIndex, int statusArgIndex, int reasonArgIndex,
            Object unarchived, Object keepArchived, String source) {
        if (param.args == null || param.args.length <= statusArgIndex
                || param.args[statusArgIndex] != unarchived) {
            return;
        }
        Object reason = param.args.length > reasonArgIndex ? param.args[reasonArgIndex] : null;
        if (isLikelyExplicitUserUnarchiveReason(reason) || isExplicitUserUnarchivePath()) {
            if (ENABLE_DEBUG_LOGS) {
                log("archive-preserve bypassed for explicit user origin in " + source
                        + "; reason=" + String.valueOf(reason));
            }
            return;
        }
        Object conversationArg = param.args.length > conversationArgIndex
                ? param.args[conversationArgIndex] : null;
        if (!hasArchivedConversationStatus(conversationArg)) {
            return;
        }
        param.args[statusArgIndex] = keepArchived;
        if (ENABLE_DEBUG_LOGS) {
            log("archive-preserve hook fired in " + source
                    + "; UNARCHIVED changed to KEEP_ARCHIVED; reason="
                    + String.valueOf(reason));
        }
    }

    private static boolean hasArchivedConversationStatus(Object conversationArg) {
        if (conversationArg == null) {
            return false;
        }
        if (conversationArg instanceof java.util.Collection) {
            boolean sawKnownUnarchived = false;
            for (Object conversationId : (java.util.Collection<?>) conversationArg) {
                int status = conversationArchiveStatusFromGlobalLookup(conversationId);
                if (status == ARCHIVE_STATUS_ARCHIVED || status == ARCHIVE_STATUS_KEEP_ARCHIVED) {
                    return true;
                }
                if (status == ARCHIVE_STATUS_UNARCHIVED) {
                    sawKnownUnarchived = true;
                }
            }
            if (!sawKnownUnarchived) {
                logOnce("archive-status-unknown-default-keep",
                        "archive status unresolved; defaulting to KEEP_ARCHIVED");
                return true;
            }
            return false;
        }
        int status = conversationArchiveStatusFromGlobalLookup(conversationArg);
        if (status == ARCHIVE_STATUS_UNKNOWN) {
            logOnce("archive-status-unknown-default-keep",
                    "archive status unresolved; defaulting to KEEP_ARCHIVED");
            return true;
        }
        return status == ARCHIVE_STATUS_ARCHIVED || status == ARCHIVE_STATUS_KEEP_ARCHIVED;
    }

    private static boolean isLikelyExplicitUserUnarchiveReason(Object reason) {
        if (!(reason instanceof Enum<?>)) {
            return false;
        }
        String name = ((Enum<?>) reason).name();
        if (TextUtils.isEmpty(name)) {
            return false;
        }
        return name.contains("FROM_LIST")
                || name.contains("FROM_MORE_BUTTON")
                || name.contains("FROM_SEARCH")
                || name.contains("FROM_DETAILS_ACTION")
                || name.contains("FROM_NOTIFICATION")
                || name.contains("FROM_WEB_ACTION")
                || name.contains("FROM_WEARABLE_ACTION")
                || name.contains("FROM_BUBBLE")
                || name.contains("FROM_COMPOSE");
    }

    private static void maybeKeepArchived(XC_MethodHook.MethodHookParam param, int statusArgIndex,
            Object unarchived, Object archived, Object keepArchived, String source) {
        if (param.args == null || param.args.length <= statusArgIndex
                || param.args[statusArgIndex] != unarchived) {
            return;
        }

        Object current = currentArchiveStatus(param.thisObject, param.args[0]);
        if (current == archived || current == keepArchived) {
            if (isExplicitUserUnarchivePath()) {
                if (ENABLE_DEBUG_LOGS) {
                    log("archive-preserve bypassed for explicit user unarchive path in " + source);
                }
                return;
            }
            param.args[statusArgIndex] = keepArchived;
            if (ENABLE_DEBUG_LOGS) {
                log("archive-preserve hook fired in " + source
                        + "; UNARCHIVED changed to KEEP_ARCHIVED");
            }
        }
    }

    private static Object currentArchiveStatus(Object metadataOps, Object conversationId) {
        try {
            Object conversationStoreProvider = XposedHelpers.getObjectField(metadataOps, "h");
            Object conversationStore = XposedHelpers.callMethod(conversationStoreProvider, "a");
            try {
                return XposedHelpers.callMethod(conversationStore, "r", conversationId);
            } catch (Throwable ignored) {
                // Older supported Messages builds used q(...) for the same archive-status lookup.
            }
            return XposedHelpers.callMethod(conversationStore, "q", conversationId);
        } catch (Throwable t) {
            logThrowable("archive-preserve current status lookup failed", t);
            return null;
        }
    }

    private static void openArchivedActivity(Context context, Object account) {
        try {
            if (account == null) {
                log("archive intent account context missing; skipping launch to avoid crash");
                return;
            }
            Activity sourceActivity = findActivity(context);
            Intent intent = new Intent();
            intent.setClassName(TARGET_PACKAGE, ARCHIVED_ACTIVITY);
            if (!populateArchiveIntentAccount(intent, account, context.getClassLoader())) {
                log("archive intent helper unavailable; skipping launch to avoid crash");
                return;
            }
            log("archive intent populated with account context");
            if (!(context instanceof Activity)) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            }
            context.startActivity(intent);
            archiveBackToInboxPendingFromTrigger = true;
            log("archive screen opened");
            if (sourceActivity != null
                    && ZERO_STATE_SEARCH_ACTIVITY.equals(sourceActivity.getClass().getName())) {
                sourceActivity.finish();
                log("search activity finished after archive open; back routes to inbox");
            }
        } catch (Throwable t) {
            logThrowable("archive screen open failed", t);
        }
    }

    private static boolean populateArchiveIntentAccount(Intent intent, Object account,
            ClassLoader classLoader) {
        Class<?> cachedHelper = archiveIntentAccountHelperClass;
        if (cachedHelper != null) {
            try {
                if (invokeArchiveIntentPopulate(cachedHelper, intent, account)) {
                    return true;
                }
            } catch (Throwable ignored) {
                archiveIntentAccountHelperClass = null;
            }
        }
        if (classLoader == null) {
            return false;
        }
        for (String helperClassName : ARCHIVE_INTENT_HELPER_CLASS_CANDIDATES) {
            try {
                Class<?> helperClass = XposedHelpers.findClass(helperClassName, classLoader);
                if (invokeArchiveIntentPopulate(helperClass, intent, account)) {
                    archiveIntentAccountHelperClass = helperClass;
                    log("runtime class cached: " + helperClassName);
                    return true;
                }
            } catch (Throwable ignored) {
                // Try next helper candidate.
            }
        }
        return false;
    }

    private static boolean invokeArchiveIntentPopulate(Class<?> helperClass, Intent intent,
            Object account) {
        if (helperClass == null || intent == null || account == null) {
            return false;
        }
        Method method = findArchiveIntentPopulateMethod(helperClass, account);
        if (method == null) {
            return false;
        }
        try {
            method.setAccessible(true);
            method.invoke(null, intent, account);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static Method findArchiveIntentPopulateMethod(Class<?> helperClass, Object account) {
        if (helperClass == null) {
            return null;
        }
        for (Method method : helperClass.getDeclaredMethods()) {
            if ((method.getModifiers() & Modifier.STATIC) == 0) {
                continue;
            }
            Class<?>[] parameterTypes = method.getParameterTypes();
            if (parameterTypes == null || parameterTypes.length != 2) {
                continue;
            }
            if (parameterTypes[0] != Intent.class
                    && !parameterTypes[0].isAssignableFrom(Intent.class)) {
                continue;
            }
            if (account != null && !parameterTypes[1].isAssignableFrom(account.getClass())) {
                continue;
            }
            return method;
        }
        return null;
    }

    private static Activity findActivity(Context context) {
        Context current = context;
        int depth = 0;
        while (current instanceof ContextWrapper && depth < 64) {
            if (current instanceof Activity) {
                return (Activity) current;
            }
            Context base = ((ContextWrapper) current).getBaseContext();
            if (base == null || base == current) {
                break;
            }
            current = base;
            depth++;
        }
        return current instanceof Activity ? (Activity) current : null;
    }

    private static boolean hideChildByName(View root, String idName) {
        int id = resourceId(root.getContext(), "id", idName);
        if (id == 0) {
            return false;
        }

        View child = root.findViewById(id);
        if (child == null || child.getVisibility() == View.GONE) {
            return false;
        }

        child.setVisibility(View.GONE);
        return true;
    }

    @SuppressLint("DiscouragedApi")
    private static int resourceId(Context context, String type, String name) {
        String key = type + ":" + name;
        Integer cached = RESOURCE_ID_CACHE.get(key);
        if (cached != null) {
            return cached;
        }

        try {
            int id = context.getResources().getIdentifier(name, type, TARGET_PACKAGE);
            clearMapIfOversized(RESOURCE_ID_CACHE, MAX_RESOURCE_ID_CACHE_SIZE);
            RESOURCE_ID_CACHE.put(key, id);
            return id;
        } catch (Throwable t) {
            logThrowable("resource lookup failed: " + type + "/" + name, t);
            return 0;
        }
    }

    private static int resourceIdFromMenuContext(Menu menu, String idName) {
        if (menu == null) {
            return 0;
        }
        for (int i = 0; i < menu.size(); i++) {
            MenuItem item = menu.getItem(i);
            if (item == null) {
                continue;
            }
            View actionView = item.getActionView();
            if (actionView != null) {
                return resourceId(actionView.getContext(), "id", idName);
            }
        }
        return 0;
    }

    private static boolean isExplicitUserUnarchivePath() {
        if (isUserUnarchiveActionActive()) {
            return true;
        }
        StackTraceElement[] trace = Thread.currentThread().getStackTrace();
        if (trace == null) {
            return false;
        }
        for (StackTraceElement frame : trace) {
            String className = frame.getClassName();
            if (className == null) {
                continue;
            }
            String methodName = frame.getMethodName();

            boolean archiveApiFrame = false;
            for (String suffix : ARCHIVE_API_IMPL_CLASS_CANDIDATES) {
                if (className.endsWith(suffix)) {
                    archiveApiFrame = true;
                    break;
                }
            }
            if (archiveApiFrame) {
                continue;
            }
            if ("onActionItemClicked".equals(methodName)) {
                for (String suffix : ARCHIVED_SELECTION_CONTROLLER_CLASS_CANDIDATES) {
                    if (className.endsWith(suffix)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static void resolveRuntimeClasses(ClassLoader classLoader) {
        for (String helperClassName : ARCHIVE_INTENT_HELPER_CLASS_CANDIDATES) {
            try {
                Class<?> helperClass = XposedHelpers.findClass(helperClassName, classLoader);
                if (findArchiveIntentPopulateMethod(helperClass, null) == null) {
                    continue;
                }
                archiveIntentAccountHelperClass = helperClass;
                log("runtime class cached: " + helperClassName);
                return;
            } catch (Throwable ignored) {
                // Try next helper class.
            }
        }
    }

    private static Class<?> findClassAny(ClassLoader classLoader, String[] classNames) {
        if (classLoader == null || classNames == null) {
            return null;
        }
        for (String className : classNames) {
            try {
                return XposedHelpers.findClass(className, classLoader);
            } catch (Throwable ignored) {
                // Try next class.
            }
        }
        return null;
    }

    private static Class<?> findInstantiableClassAny(ClassLoader classLoader, String[] classNames) {
        if (classLoader == null || classNames == null) {
            return null;
        }
        for (String className : classNames) {
            try {
                Class<?> candidate = XposedHelpers.findClass(className, classLoader);
                XposedHelpers.newInstance(candidate);
                return candidate;
            } catch (Throwable ignored) {
                // Try next class.
            }
        }
        return null;
    }

    private static Object buildImmediateFuture(ClassLoader classLoader, Object value) {
        try {
            Class<?> futuresClass = XposedHelpers.findClass(
                    "com.google.common.util.concurrent.Futures", classLoader);
            return XposedHelpers.callStaticMethod(futuresClass, "immediateFuture", value);
        } catch (Throwable ignored) {
            // Fall back to known internal helper classes.
        }
        try {
            Class<?> futureClass = XposedHelpers.findClass("fitm", classLoader);
            return XposedHelpers.callStaticMethod(futureClass, "i", value);
        } catch (Throwable ignored) {
            // Continue fallback.
        }
        try {
            Class<?> futureClass = XposedHelpers.findClass("fkze", classLoader);
            return XposedHelpers.callStaticMethod(futureClass, "i", value);
        } catch (Throwable ignored) {
            // Continue fallback.
        }
        try {
            Class<?> futureClass = XposedHelpers.findClass("feum", classLoader);
            return XposedHelpers.callStaticMethod(futureClass, "e", value);
        } catch (Throwable ignored) {
            // Continue fallback.
        }
        try {
            Class<?> futureClass = XposedHelpers.findClass("fekd", classLoader);
            return XposedHelpers.callStaticMethod(futureClass, "e", value);
        } catch (Throwable ignored) {
            // Continue fallback.
        }
        try {
            Class<?> futureClass = XposedHelpers.findClass("fept", classLoader);
            return XposedHelpers.callStaticMethod(futureClass, "e", value);
        } catch (Throwable ignored) {
            // Continue fallback.
        }
        try {
            Class<?> futureClass = XposedHelpers.findClass("fcrp", classLoader);
            return XposedHelpers.callStaticMethod(futureClass, "e", value);
        } catch (Throwable ignored) {
            // Continue fallback.
        }
        try {
            Class<?> futureClass = XposedHelpers.findClass("feiu", classLoader);
            return XposedHelpers.callStaticMethod(futureClass, "e", value);
        } catch (Throwable ignored) {
            // Continue fallback.
        }
        try {
            if (value instanceof Iterable) {
                Class<?> futureClass = XposedHelpers.findClass("fitm", classLoader);
                return XposedHelpers.callStaticMethod(futureClass, "e", value);
            }
        } catch (Throwable ignored) {
            // Continue fallback.
        }
        try {
            Class<?> futureClass = XposedHelpers.findClass("fbpi", classLoader);
            return XposedHelpers.callStaticMethod(futureClass, "e", value);
        } catch (Throwable ignored) {
            // Continue fallback.
        }
        try {
            Class<?> futureClass = XposedHelpers.findClass("fbje", classLoader);
            return XposedHelpers.callStaticMethod(futureClass, "e", value);
        } catch (Throwable ignored) {
            // Continue fallback.
        }
        try {
            Class<?> futureClass = XposedHelpers.findClass("fboy", classLoader);
            return XposedHelpers.callStaticMethod(futureClass, "e", value);
        } catch (Throwable ignored) {
            // No more fallback options.
        }
        return null;
    }

    private static Object readFieldIfPresent(Object target, String fieldName) {
        if (target == null || fieldName == null) {
            return null;
        }
        try {
            return XposedHelpers.getObjectField(target, fieldName);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static boolean applyProfileActionHidden(Object action,
            Class<?> hiddenVisibilityHandlerClass) {
        if (action == null || hiddenVisibilityHandlerClass == null) {
            return false;
        }

        final Object hiddenHandler;
        try {
            hiddenHandler = XposedHelpers.newInstance(hiddenVisibilityHandlerClass);
        } catch (Throwable t) {
            logThrowable("profile archived hide: hidden handler instantiation failed", t);
            return false;
        }

        if (setObjectFieldIfPresent(action, "g", hiddenHandler)) {
            return true;
        }
        if (setObjectFieldIfPresent(action, "e", hiddenHandler)) {
            return true;
        }

        // Fallback for future obfuscation: assign to any compatible visibility field.
        for (Field field : action.getClass().getDeclaredFields()) {
            try {
                if (!field.getType().isAssignableFrom(hiddenVisibilityHandlerClass)) {
                    continue;
                }
                field.setAccessible(true);
                field.set(action, hiddenHandler);
                return true;
            } catch (Throwable ignored) {
                // Continue probing compatible fields.
            }
        }
        return false;
    }

    private static boolean setObjectFieldIfPresent(Object target, String fieldName, Object value) {
        try {
            XposedHelpers.setObjectField(target, fieldName, value);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static void clearSetIfOversized(Set<?> set, int maxSize) {
        if (set == null || maxSize <= 0) {
            return;
        }
        if (set.size() >= maxSize) {
            set.clear();
        }
    }

    private static void clearMapIfOversized(Map<?, ?> map, int maxSize) {
        if (map == null || maxSize <= 0) {
            return;
        }
        if (map.size() >= maxSize) {
            map.clear();
        }
    }

    private static void logOnce(String key, String message) {
        if (!ENABLE_DEBUG_LOGS) {
            return;
        }
        clearSetIfOversized(ONCE_LOG_KEYS, MAX_ONCE_LOG_KEYS);
        if (ONCE_LOG_KEYS.add(key)) {
            log(message);
        }
    }

    private static void log(String message) {
        if (!ENABLE_DEBUG_LOGS) {
            return;
        }
        XposedBridge.log(TAG + ": " + message);
        Log.d(TAG, message);
        appendRuntimeLog("D " + message);
    }

    private static void logThrowable(String message, Throwable throwable) {
        if (!ENABLE_ERROR_LOGS) {
            return;
        }
        String fullMessage = message + ": " + throwable;
        XposedBridge.log(TAG + ": " + fullMessage);
        XposedBridge.log(throwable);
        Log.e(TAG, fullMessage, throwable);
        appendRuntimeLog("E " + fullMessage);
    }
}
