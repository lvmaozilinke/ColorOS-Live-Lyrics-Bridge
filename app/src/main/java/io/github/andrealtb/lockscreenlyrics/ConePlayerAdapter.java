package io.github.andrealtb.lockscreenlyrics;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collections;

import io.github.libxposed.api.XposedInterface;

import org.luckypray.dexkit.DexKitBridge;
import org.luckypray.dexkit.query.FindClass;
import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.query.matchers.ClassMatcher;
import org.luckypray.dexkit.query.matchers.MethodMatcher;
import org.luckypray.dexkit.result.ClassData;
import org.luckypray.dexkit.result.ClassDataList;
import org.luckypray.dexkit.result.MethodData;
import org.luckypray.dexkit.result.MethodDataList;

final class ConePlayerAdapter implements PlayerAdapter {
    private static final String SOURCE_NAME = "ConePlayer parser";
    private static final String METADATA_PATTERN = "\\[(\\w{2,3}):(.+?)]";
    private static final String TIMESTAMP_PATTERN =
            "\\[(\\d{2}):(\\d{2})(?:\\.(\\d{1,3}))?]";
    private static final String BACKGROUND_LYRIC_PATTERN = "\\[bg:[^\\]]*?\\]";
    private static final Object DEXKIT_LOAD_LOCK = new Object();

    private static volatile boolean dexKitLoaded;

    private final String packageName;

    ConePlayerAdapter(String packageName) {
        this.packageName = packageName;
    }

    @Override
    public String packageName() {
        return packageName;
    }

    @Override
    public String displayName() {
        return "ConePlayer";
    }

    @Override
    public LyricProviderCapabilities lyricCapabilities() {
        return LyricProviderCapabilities.PASSIVE_PARSER;
    }

    @Override
    public void installLyricSourceHooks(LockscreenLyricsModule module, ClassLoader classLoader) {
        module.installInjectedTranslationToggleActionHook(packageName);

        try {
            ensureDexKitLoaded();
        } catch (Throwable t) {
            module.error("Failed to load DexKit for " + displayName(), t);
            return;
        }

        try (DexKitBridge bridge = DexKitBridge.create(classLoader, true)) {
            Method parserMethod = findLrcParserMethod(bridge, classLoader);
            parserMethod.setAccessible(true);
            module.hook(parserMethod)
                    .setId("cone-player-lrc-parser-" + packageName)
                    .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                    .intercept(chain -> onParseLyric(
                            module,
                            chain,
                            lyricCapabilities()));
            module.info("Hooked " + displayName() + " lyric parser via DexKit: "
                    + parserMethod.getDeclaringClass().getName() + "#" + parserMethod.getName());
        } catch (Throwable t) {
            module.error("Failed to hook " + displayName() + " lyric parser via DexKit", t);
        }
    }

    private static void ensureDexKitLoaded() {
        if (dexKitLoaded) {
            return;
        }
        synchronized (DEXKIT_LOAD_LOCK) {
            if (dexKitLoaded) {
                return;
            }
            System.loadLibrary("dexkit");
            dexKitLoaded = true;
        }
    }

    private static Method findLrcParserMethod(
            DexKitBridge bridge,
            ClassLoader classLoader) throws ReflectiveOperationException {
        ClassDataList parserClasses = bridge.findClass(FindClass.create()
                .matcher(ClassMatcher.create()
                        .usingEqStrings(
                                METADATA_PATTERN,
                                TIMESTAMP_PATTERN,
                                BACKGROUND_LYRIC_PATTERN)));
        ClassData parserClass = requireSingleClass(parserClasses);

        MethodDataList parserMethods = findParserMethods(
                bridge,
                parserClass,
                "java.lang.String",
                "boolean");
        if (parserMethods.isEmpty()) {
            parserMethods = findParserMethods(
                    bridge,
                    parserClass,
                    "java.lang.String");
        }
        MethodData parserMethod = requireSingleMethod(parserMethods);
        return parserMethod.getMethodInstance(classLoader);
    }

    private static MethodDataList findParserMethods(
            DexKitBridge bridge,
            ClassData parserClass,
            String... parameterTypes) {
        return bridge.findMethod(FindMethod.create()
                .searchInClass(Collections.singletonList(parserClass))
                .matcher(MethodMatcher.create()
                        .modifiers(Modifier.STATIC)
                        .returnType("java.util.List")
                        .paramTypes(parameterTypes)));
    }

    private static ClassData requireSingleClass(ClassDataList classes) {
        if (classes.size() == 1) {
            return classes.get(0);
        }
        throw new IllegalStateException(
                "Expected one ConePlayer LRC parser class, found " + classes.size()
                        + ": " + classes);
    }

    private static MethodData requireSingleMethod(MethodDataList methods) {
        if (methods.size() == 1) {
            return methods.get(0);
        }
        throw new IllegalStateException(
                "Expected one ConePlayer LRC parser method, found " + methods.size()
                        + ": " + methods);
    }

    private static Object onParseLyric(
            LockscreenLyricsModule module,
            XposedInterface.Chain chain,
            LyricProviderCapabilities capabilities) throws Throwable {
        Object rawLyricArg = chain.getArg(0);
        Object result = chain.proceed();
        if (rawLyricArg instanceof String) {
            module.cacheTimedLyric(
                    SOURCE_NAME,
                    (String) rawLyricArg,
                    capabilities);
        }
        return result;
    }
}
