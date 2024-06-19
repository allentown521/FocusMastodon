#############默认混淆配置####################
#压缩（shrinking）：用来减少应用体积，移除未使用的类和成员,开启后如果有native方法没有使用会被移除，然后运行时会报错，关闭apk大小增加了太多
#-dontshrink
#混淆过程中打印详细信息
-verbose
-renamesourcefileattribute SourceFile
-keepattributes *Annotation*, SourceFile, InnerClasses, LineNumberTable, Signature, EnclosingMethod
-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*    #优化 混淆时采用的算法

-keep,allowshrinking class * extends android.app.Activity

-keep,allowshrinking class * extends android.app.Service

-keep public class * extends android.app.Activity    # 未指定成员，仅仅保持类名不被混淆
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.app.IntentService
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.app.backup.BackupAgentHelper
-keep public class * extends android.preference.Preference
-keep public class * extends android.hardware.display.DisplayManager
-keep public class * extends android.os.UserManager
-keep public class * extends android.app.Fragment
-keep public class * extends androidx.fragment.app.Fragment
-keep public class * extends androidx.viewpager.widget.PagerAdapter
-keep public class * extends androidx.recyclerview.widget.RecyclerView$Adapter


-keep class com.google.android.material.** {*;}
-keep class androidx.** {*;}
-keep public class * extends androidx.**
-keep interface androidx.** {*;}
-dontwarn com.google.android.material.**
-dontnote com.google.android.material.**
-dontwarn androidx.**


-keep public class * extends android.support.v4.**    #  *匹配任意字符不包括.  **匹配任意字符
-keep interface android.support.v4.app.** { *; }    #{ *;}    表示一个接口中的所有的东西都不被混淆
# 下面这行表示保持这个包下面的所有的类里面的所有内容都不混淆
-keep class android.support.v4.** { *; }
-keep class android.os.**{*;}
-keep class android.support.v8.renderscript.** { *; }

-keep class **.R$* { *; }
-keep class **.R{ *; }

#实现了android.os.Parcelable接口类的任何类，以及其内部定义的Creator内部类类型的public final静态成员变量，都不能被混淆和删除
-keep class * implements android.os.Parcelable {    # 保持Parcelable不被混淆
  public static final android.os.Parcelable$Creator *;
}
#成员变量也不能混淆，可能会持久化到文件中去
-keepclassmembers class * implements android.os.Parcelable {
 public <fields>;
 private <fields>;
}

#keepclasseswithmembers和 keepclasseswithmembernames区别，-keepclassmembers 防止成员被移除或者被重命名；-keepclasseswithmembernames防止被重命名
-keepclassmembers class * {     # 保持 native 方法不被混淆以及被移除（未使用，即使未使用如果移除了运行时还是会报错）
    native <methods>;
}

-keepclasseswithmembers class * {         # 保持自定义控件类不被混淆
    public <init>(android.content.Context, android.util.AttributeSet);
}

-keepclasseswithmembers class * {         # 保持自定义控件类不被混淆
    public <init>(android.content.Context, android.util.AttributeSet, int);
}

-keepclasseswithmembers class * {
  public <init>(android.content.Context, android.util.AttributeSet, int, int);
}

-keepclassmembers class * extends android.app.Activity { #保持类成员
   public void *(android.view.View);
}

-keepclassmembers class * extends android.content.Context {
  public void *(android.view.View);
  public void *(android.view.MenuItem);
}

-keepclassmembers enum * {                  # 保持枚举 enum 类不被混淆
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Explicitly preserve all serialization members. The Serializable interface
# is only a marker interface, so it wouldn't save them.
-keepnames class * implements java.io.Serializable

-keepclassmembers class * implements java.io.Serializable {
  static final long serialVersionUID;
  private static final java.io.ObjectStreamField[] serialPersistentFields;
  private void writeObject(java.io.ObjectOutputStream);
  private void readObject(java.io.ObjectInputStream);
  java.lang.Object writeReplace();
  java.lang.Object readResolve();
}


# Understand the @Keep support annotation.
-keep class androidx.annotation.Keep

-keep @androidx.annotation.Keep class * {*;}

#不混淆@Keep下面的子类,不知道有没有用
-keep @androidx.annotation.Keep class ** {*;}

-keepclasseswithmembers class * {
    @androidx.annotation.Keep <methods>;
}

-keepclasseswithmembers class * {
    @androidx.annotation.Keep <fields>;
}

-keepclasseswithmembers class * {
    @androidx.annotation.Keep <init>(...);
}



#-----------------上面为通用混淆规则------------------------


# Keep methods used in tests.
# This is only needed when running tests with proguard enabled.
-keepclassmembers class org.apache.commons.lang3.StringUtils {*;}
-keepclassmembers class androidx.appcompat.app.ActionBar {
    public ** getTitle();
}
-keepclassmembers class org.apache.commons.io.IOUtils {
    public static void write(...);
}

-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

-keepclassmembers class * implements android.os.Parcelable {
    static android.os.Parcelable$Creator CREATOR;
}

-keep public class org.jsoup.** {
    public *;
}

# for okhttp
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase
-dontwarn org.codehaus.mojo.animal_sniffer.*

# for RxJava RxAndroid
-dontwarn sun.misc.Unsafe
-dontwarn sun.misc.**
-keepclassmembers class rx.internal.util.unsafe.*ArrayQueue*Field* {
    long producerIndex;
    long consumerIndex;
}
-keepclassmembers class rx.internal.util.unsafe.BaseLinkedQueueProducerNodeRef {
    rx.internal.util.atomic.LinkedQueueNode producerNode;
}
-keepclassmembers class rx.internal.util.unsafe.BaseLinkedQueueConsumerNodeRef {
    rx.internal.util.atomic.LinkedQueueNode consumerNode;
}

-dontnote rx.internal.util.PlatformDependent
-keep class rx.schedulers.Schedulers {
    public static <methods>;
}
-keep class rx.schedulers.Schedulers {
    public static ** test();
}
-keep class rx.schedulers.ImmediateScheduler {
    public <methods>;
}
-keep class rx.schedulers.TestScheduler {
    public <methods>;
}


# for retrolambda
-dontwarn java.lang.invoke.*

# greenrobot EventBus
-keepattributes *Annotation*
-keepclassmembers class * {
    @org.greenrobot.eventbus.Subscribe <methods>;
}
-keep enum org.greenrobot.eventbus.ThreadMode { *; }

# android-iconify
-keep class com.joanzapata.** { *; }

# Glide
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep public class * extends com.bumptech.glide.module.AppGlideModule
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** {
  **[] $VALUES;
  public *;
}
-dontwarn com.bumptech.glide.load.resource.bitmap.VideoDecoder

# for ViewPageIndicator problems (https://github.com/JakeWharton/ViewPagerIndicator/issues/366):
-dontwarn com.viewpagerindicator.LinePageIndicator


# Retrofit 2.0
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepattributes Signature
-keepattributes Exceptions

-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# Moshi
-keep class com.squareup.moshi.** { *; }
-keep interface com.squareup.moshi.** { *; }
-keep public class retrofit2.adapter.rxjava.RxJavaCallAdapterFactory { *; }

# awaitility
-dontwarn java.beans.BeanInfo
-dontwarn java.beans.Introspector
-dontwarn java.beans.IntrospectionException
-dontwarn java.beans.PropertyDescriptor
-dontwarn java.lang.management.ManagementFactory
-dontwarn java.lang.management.ThreadInfo
-dontwarn java.lang.management.ThreadMXBean

#for zt-zip
-dontwarn java.nio.**
-keep class java.nio.** { *; }
-dontwarn java.util.zip**
-keep class java.util.zip.** { *; }
-dontwarn org.slf4j.impl**
-keep class org.slf4j.impl.** { *; }

# Gson specific classes
-keep class sun.misc.Unsafe { *; }
#-keep class com.google.gson.stream.** { *; }

# Application classes that will be serialized/deserialized over Gson
-keep class com.google.gson.examples.android.model.** { *; }


#alipay org也不能混淆
-keep class com.alipay.** { *; }
-dontwarn org.apache.**
-keep class org.apache.** { *; }

# Fix OAuth Drive API failure for release builds
-keep class * extends com.google.api.client.json.GenericJson { *; }
-keep class com.google.api.services.drive.** { *; }
-keepclassmembers class * { @com.google.api.client.util.Key <fields>; }

-dontwarn com.amazon.**
-keep class com.amazon.** {*;}
-keepattributes *注释*

# Understand the @Keep support annotation.
-keep class androidx.annotation.Keep

-keep @androidx.annotation.Keep class * {*;}

#不混淆@Keep下面的子类,不知道有没有用
-keep @androidx.annotation.Keep class ** {*;}

-keepclasseswithmembers class * {
    @androidx.annotation.Keep <methods>;
}

-keepclasseswithmembers class * {
    @androidx.annotation.Keep <fields>;
}

-keepclasseswithmembers class * {
    @androidx.annotation.Keep <init>(...);
}

# Understand the @Keep support annotation.
-keep class allen.town.focus_common.http.data.AutoGson

-keep @allen.town.focus_common.http.data.AutoGson class * {*;}

-keepclasseswithmembers class * {
    @allen.town.focus_common.http.data.AutoGson <methods>;
}

-keepclasseswithmembers class * {
    @allen.town.focus_common.http.data.AutoGson <fields>;
}

-keepclasseswithmembers class * {
    @allen.town.focus_common.http.data.AutoGson <init>(...);
}

#Arouter
-keep public class com.alibaba.android.arouter.routes.**{*;}
-keep public class com.alibaba.android.arouter.facade.**{*;}
-keep class * implements com.alibaba.android.arouter.facade.template.ISyringe{*;}

# 如果使用了 byType 的方式获取 Service，需添加下面规则，保护接口
-keep interface * implements com.alibaba.android.arouter.facade.template.IProvider

# 如果使用了 单类注入，即不定义接口实现 IProvider，需添加下面规则，保护实现
-keep class * implements com.alibaba.android.arouter.facade.template.IProvider
-keep class * implements com.alibaba.android.arouter.facade.template.IInterceptor{*;}
#因为Autowired需要用到其所在类的类名等信息，所以使用Autowired的类也不能被混淆。
 #比如classA用到了Autowired注入, 如果在非Activity类中使用了@Autowired 来进行注入，需添加下面规则，防止找不到对应的 被注解类名$$ARouter$$Autowired 来进行注入
-keep class com.alibaba.android.arouter.facade.annotation.Autowired
-keepclasseswithmembers class * {
    @com.alibaba.android.arouter.facade.annotation.Autowired <fields>;
}


# Rome lib
-keep class com.rometools.** { *; }
-dontwarn java.beans.**
-dontwarn javax.**
-dontwarn org.jaxen.**
-dontwarn org.slf4j.**

#google 支付
-keep class com.android.vending.billing.** { *; }
-keep class com.google.billing.model.** { *; }
-keep class com.farsitel.bazaar.**
-keep class com.android.vending.billing.**

#alipay
-keep class com.alipay.** { *; }

# for glide4
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class * extends com.bumptech.glide.module.AppGlideModule {
 <init>(...);
}
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** {
  **[] $VALUES;
  public *;
}
-keep class com.bumptech.glide.load.data.ParcelFileDescriptorRewinder$InternalRewinder {
  *** rewind();
}

# for DexGuard only
#-keepresourcexmlelements manifest/application/meta-data@value=GlideModule

# 从glide4.0开始，GifDrawable没有提供getDecoder()方法，
# 需要通过反射获取gifDecoder字段值，所以需要保持GifFrameLoader和GifState类不被混淆
-keep class com.bumptech.glide.load.resource.gif.GifDrawable$GifState{*;}
-keep class com.bumptech.glide.load.resource.gif.GifFrameLoader {*;}

#fyyd 有json实体类
-keep class de.mfietz.fyydlin.* { *; }

# OkHttp
-keepattributes Signature
-keepattributes *Annotation*
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**

# Rome lib
-keep class com.rometools.** { *; }
-dontwarn java.beans.**
-dontwarn javax.**
-dontwarn org.jaxen.**
-dontwarn org.slf4j.**

#口袋广告
-keep class com.zh.pocket.** {*;}

#Ucrop
-dontwarn com.yalantis.ucrop**
-keep class com.yalantis.ucrop** { *; }
-keep interface com.yalantis.ucrop** { *; }

#twitter4j
-keep class twitter4j.**  { *; }
-dontwarn twitter4j.**
-keep  class twitter4j.conf.PropertyConfigurationFactory

# Parceler library
-keep interface org.parceler.Parcel
-keep @org.parceler.Parcel class * { *; }
-keep class **$$Parcelable { *; }

# https://github.com/bluelinelabs/LoganSquare
-keep class com.bluelinelabs.logansquare.annotation.JsonObject
-keep class * extends com.bluelinelabs.logansquare.JsonMapper
-keep @com.bluelinelabs.logansquare.annotation.JsonObject class *

# https://github.com/mariotaku/RestFu
-keep class org.mariotaku.restfu.annotation.** { *; }

-keep class * extends org.mariotaku.library.objectcursor.ObjectCursor$CursorIndices

# Keep all enums for debugging purposes
-keepnames public enum * {
	*;
}

-keepclassmembers,allowobfuscation class * {
  @com.google.gson.annotations.SerializedName <fields>;
  @com.squareup.otto.Subscribe <methods>;
}

# Keep all model classes as they're used with gson and their names are shown in errors
-keep public class allen.town.focus.twitter.model.**{
	<fields>;
}

# Inner classes in api requests are used with gson
-keepclassmembers class allen.town.focus.twitter.api.**$*{
	*;
}

# Prevent proguard from stripping interface information from TypeAdapter, TypeAdapterFactory,
# JsonSerializer, JsonDeserializer instances (so they can be used in @JsonAdapter)
-keep class * extends com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Retain generic signatures of TypeToken and its subclasses with R8 version 3.0 and higher.
-keep,allowobfuscation,allowshrinking class com.google.gson.reflect.TypeToken
-keep,allowobfuscation,allowshrinking class * extends com.google.gson.reflect.TypeToken

# Retain generic signatures of classes used in MastodonApi so Retrofit works
-keep,allowobfuscation,allowshrinking class io.reactivex.Single
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking class kotlin.collections.List
-keep,allowobfuscation,allowshrinking class kotlin.collections.Map
-keep,allowobfuscation,allowshrinking class retrofit2.Call

# https://r8.googlesource.com/r8/+/refs/heads/master/compatibility-faq.md#retrofit
-keepattributes Signature
-keep class kotlin.coroutines.Continuation

#or android.enableR8.fullMode=false
#https://r8.googlesource.com/r8/+/refs/heads/master/compatibility-faq.md#troubleshooting
-keep class com.google.gson.reflect.TypeToken
-keep class * extends com.google.gson.reflect.TypeToken
-keep public class * implements java.lang.reflect.Type

# This is generated automatically by the Android Gradle plugin.
-dontwarn com.beloo.widget.chipslayoutmanager.Orientation
-dontwarn com.bumptech.glide.DrawableRequestBuilder
-dontwarn com.bumptech.glide.DrawableTypeRequest
-dontwarn com.bumptech.glide.GenericRequestBuilder
-dontwarn com.bumptech.glide.GenericTranscodeRequest
-dontwarn com.bumptech.glide.RequestManager$GenericModelRequest$GenericTypeRequest
-dontwarn com.bumptech.glide.RequestManager$GenericModelRequest
-dontwarn com.bumptech.glide.load.model.stream.StreamStringLoader
-dontwarn com.google.android.exoplayer2.source.rtsp.RtspMessageChannel$MessageParser$ReadingState
-dontwarn com.potyvideo.library.BR
-dontwarn org.jspecify.nullness.Nullable