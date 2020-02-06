![logo](./img/koin_2.0.jpg)

## What is KOIN-MP?

KOIN is "A pragmatic lightweight dependency injection framework for Kotlin developers."

KOIN-MP is a fork of KOIN designed for use in a multiplatform environment. It differs from KOIN and other multiplatform DI options in that it is explicit about what state is mutable and from what threads you can perform operations.

Primarily the goal is to create a DI framework that works well within Kotlin/Native's state and concurrency model, but also acts the same when being used from the JVM. Native's state model is an attempt to ensure safe mutability. If you are able to follow it's rules in the JVM, that's probably a good thing.

KOIN-MP is experimental. As we're rethinking architecture with Native, we're rethinking state and mutability. Details may change over time. Also, the hope is that ideas from KOIN-MP are incorporated back into KOIN and we can have just "KOIN", but I'd rather experiment first then discuss, so we're starting with a fork.

## What are the rules?

All mutable state in KOIN-MP is restricted to the main thread internally.

1. **All module config needs to happen on the main thread**.
2. **All inject/get operations happen in the main thread *by default***. 

If you configure your Koin instance in the main thread, and inject in the main thread, then KOIN-MP is syntactically the same as KOIN.

You can allow inject/get from other threads, but you must configure your components to specially allow that with the `ThreadScope` parameter.

```kotlin
single(threadScope = ThreadScope.Shared) { Simple.Component1() }
```

This will allow you to inject the single from a background thread.

Trying to configure or inject/get from a different thread will throw an exception, except for the `ThreadScope` option above. In that case, it will call to the main thread to run the inject/get. If your code does a lot of getting from background threads, this fork may not be for you.

## Why those restrictions?

KN's state rules formalize thread confinement as a way of ensuring safe concurrency. In short, that means all mutable state is confined to a single thread. To a native mobile developer, this is intuitive, because both Android and iOS have a concept of "main thread". You're only allowed to change the UI from the main thread. As a consequence, on native mobile, the vast majority of DI happens in the main thread.

Koin internally has a few moving parts that will change over time. That state needs to be mutable but safe concurrent. Isolating all access to the main thread makes the state concurrency safe, and in the primary use case (main thread), very performant, because the main thread has immediate access, and there's no synchronization or locks.

Enforcing the same rules on all platforms makes Koin uniform, rather than having different platforms behave differently. KN's concurrency is a good idea, and those best practices are often self imposed on the jvm. We're applying the same principle here.

On native, one of the primary benefits is you don't have to freeze `single` instances. You *can*, but you don't *need* to. In earlier Koin multiplatform versions, all state was internally frozen. We are moving away from freezing everything and being intentional about our state. See [my talk](https://youtu.be/oxQ6e1VeH4M?t=1078).

## Bad News

Only Android and iOS have a formal "main thread". Linux, windows, raw JVM, etc have no default main thread, so there's no way to "run something on the main thread". As a result, on those platforms, you can only access Koin on the first thread you touch it with, and cannot inject/get from another thread, even if you configure it.

For server use cases, the main thread restriction is problematic at best. There are generally thread pools servicing many requests. We'll want the JVM-only version of Koin to be more thread-agnostic, but for today I've shelved that to focus on testing the native mobile version.

Publications need to be fixed. The `aar` files aren't pushing properly. That's a pretty short term problem, though.

## Original KOIN Docs

A pragmatic lightweight dependency injection framework for Kotlin developers.

Written in pure Kotlin, using functional resolution only: no proxy, no code generation, no reflection.

`Koin is a DSL, a light container and a pragmatic API`

## Official Website 👉 [https://insert-koin.io](https://insert-koin.io)

### Latest News 🌐

- Follow us on Twitter for latest news: [@insertkoin_io](https://twitter.com/insertkoin_io)
- Koin developers on Medium: [koin developers hub](https://medium.com/koin-developers)

### Getting Help 🚒

Documentation:
* [Getting Stared](https://start.insert-koin.io/)
* [Documentation References](https://doc.insert-koin.io/)

Any question about Koin usage?
- Come talk on slack [#koin](https://kotlinlang.slack.com/?redir=%2Fmessages%2Fkoin) channel
- Post your question on [Stackoverflow - #koin tag](https://stackoverflow.com/questions/tagged/koin)

### Articles, Videos & other resources 🎉

Online resources about Koin - [Talking about Koin](https://github.com/InsertKoinIO/koin/blob/master/README.md#talking-about-koin)

### Reporting issues 🚑

Found a bug or a problem on a specific feature? Open an issue on [Github issues](https://github.com/InsertKoinIO/koin/issues)


### Contributing 🛠

Want to help or share a proposal about Koin? problem on a specific feature?

- Open an issue to explain the issue you want to solve [Open an issue](https://github.com/InsertKoinIO/koin/issues)
- Come talk on slack [#koin-dev](https://kotlinlang.slack.com/?redir=%2Fmessages%2Fkoin-dev) channel
- After discussion to validate your ideas, you can open a PR or even a draft PR if the contribution is a big one [Current PRs](https://github.com/InsertKoinIO/koin/pulls)

Additional readings about basic setup: https://github.com/InsertKoinIO/koin/blob/master/CONTRIBUTING.adoc


# Setup

## Current Version

```gradle
// latest stable
koin_version = '2.0.1'
// latest unstable
koin_version = '2.1.0-alpha-10'
```

## Gradle

### Jcenter

Check that you have the `jcenter` repository.

```gradle
// Add Jcenter to your repositories if needed
repositories {
	jcenter()    
}
```

### Dependencies

Pick one of your Koin dependency:

#### Core features

```gradle
// Koin for Kotlin
implementation "org.koin:koin-core:$koin_version"
// Koin extended & experimental features
implementation "org.koin:koin-core-ext:$koin_version"
// Koin for Unit tests
testImplementation "org.koin:koin-test:$koin_version"
// Koin for Java developers
implementation "org.koin:koin-java:$koin_version"
```

#### Android

```gradle
// Koin for Android
implementation "org.koin:koin-android:$koin_version"
// Koin Android Scope features
implementation "org.koin:koin-android-scope:$koin_version"
// Koin Android ViewModel features
implementation "org.koin:koin-android-viewmodel:$koin_version"
// Koin Android Experimental features
implementation "org.koin:koin-android-ext:$koin_version"
```

#### AndroidX

```gradle

// Koin AndroidX Scope features
implementation "org.koin:koin-androidx-scope:$koin_version"
// Koin AndroidX ViewModel features
implementation "org.koin:koin-androidx-viewmodel:$koin_version"
// Koin AndroidX Fragment features
implementation "org.koin:koin-androidx-fragment:$koin_version"
// Koin AndroidX Experimental features
implementation "org.koin:koin-androidx-ext:$koin_version"
```

#### Ktor

```gradle
// Koin for Ktor Kotlin
implementation "org.koin:koin-ktor:$koin_version"
```

# Quickstart


## Declare a Koin module

Write with the Koin DSL what you need to assemble:

```kotlin
// Given some classes
class Controller(val service : BusinessService)
class BusinessService()

// just declare it
val myModule = module {
  single { Controller(get()) }
  single { BusinessService() }
}
```

## Starting Koin

Use the startKoin() function to start Koin in your application.

In a Kotlin app:

```kotlin
fun main(vararg args : String) {
  // start Koin!
  startKoin {
    // your modules
    modules(myModule)
  }
}
```

In an Android app:

```kotlin
class MyApplication : Application() {
  override fun onCreate(){
    super.onCreate()
    // start Koin!
    startKoin {
      // Android context
      androidContext(this@MyApplication)
      // modules
      modules(myModule)
    }
  }
}
```

# Talking about Koin

### Articles

* [Dagger is dead. Long live Koin](https://www.linkedin.com/pulse/dagger-dead-long-live-koin-yordan-olave-cordero)
* [Testing a Koin application with KotlinTest](https://dev.to/kerooker/testing-koin-applications-with-kotlintest-1iip)
* [Ready for Koin 2.0](https://medium.com/koin-developers/ready-for-koin-2-0-2722ab59cac3)
* [Migration from Dagger2 to Koin](https://proandroiddev.com/migrating-from-dagger2-to-koin-3b2b3f5285e9)
* [From Dagger to Koin, a step by step migration guide](https://medium.com/@giuliani.arnaud/the-thermosiphon-app-from-dagger-to-koin-step-by-step-a09af7f5b5b1)
* [Koin in Feature Modules Project](https://proandroiddev.com/koin-in-feature-modules-project-6329f069f943)
* [A brief look at Koin on Android](https://overflow.buffer.com/2018/09/13/a-brief-look-at-koin-on-android/)
* [Bye bye Dagger](https://medium.com/@charbgr/bye-bye-dagger-1494118dcd41)
* [Testing with Koin](https://proandroiddev.com/testing-with-koin-ade8a46eb4d)
* [Painless Android testing with Room & Koin](https://android.jlelse.eu/painless-android-testing-with-room-koin-bb949eefcbee)
* [Unlock your Android ViewModel power with Koin](https://medium.com/@giuliani.arnaud/unlock-your-android-viewmodel-power-with-koin-23eda8f493be)
* [Using dependency injection with Koin](https://medium.com/mindorks/using-dependency-injection-with-koin-bee0b461714a)
* [Koin + Spark = ❤️]()
* [Push SparkJava to the next level](https://medium.com/koin-developers/pushing-sparkjava-to-the-next-level-with-koin-ed1f0b80953e) ([Kotlin Weekly issue 73](http://mailchi.mp/kotlinweekly/kotlin-weekly-73), [DZone.com](https://dzone.com/articles/push-sparkjava-to-the-next-level-with-koin) )
* [When Koin met Ktor ...](https://medium.com/koin-developers/when-koin-met-ktor-c3b2395662bf) ([Kotlin Weekly issue 72](https://us12.campaign-archive.com/?u=f39692e245b94f7fb693b6d82&id=3135ae0cf5))
* [Android Dependency Injection – Why we moved from Dagger 2 to Koin?](https://www.nan-labs.com/blog/android-dependency-injection-moved-dagger-2-koin/)
* [Moving from Dagger to Koin - Simplify your Android development](https://medium.com/@giuliani.arnaud/moving-from-dagger-to-koin-simplify-your-android-development-e8c61d80cddb) - ([Kotlin Weekly issue 66](http://mailchi.mp/kotlinweekly/kotlin-weekly-66?e=e8a57c719f) & [Android Weekly issue 282](http://androidweekly.net/issues/issue-282))
* [Kotlin Weekly #64](http://mailchi.mp/kotlinweekly/kotlin-weekly-64?e=e8a57c719f)
* [Insert Koin for dependency injection]()
* [Better dependency injection for Android](https://proandroiddev.com/better-dependency-injection-for-android-567b93353ad)

### Videos & Podcasts

* [Dependency Injection from zero to hero with Koin, Kotliners 2019 & Budapest, Hungary](https://www.youtube.com/watch?v=mt9yoWScgb8&list=PLnYRVL0Cw1FSUJ-WdhV2Ija9kA9q0qP3e&index=6)
* [Dependency Injection from zero to hero with Koin, AndroidMakers 2019 & Paris, France](https://www.youtube.com/watch?v=chCsNkjotfc)
* [Simplify your Android development with Koin - Mobilization @ Lodz, Poland](https://www.youtube.com/watch?v=KzQbJFVjr9w&t=1s)
* [Talking Kotlin - Dependency injection with Koin](http://talkingkotlin.com/dependency-injection-with-koin/)
* [L'injection de poireaux avec Koin - AndroidLeaks ep42 (French)](https://androidleakspodcast.com/2018/08/05/episode-42-linjection-de-poireaux-avec-koin/)
* [Insert Koin. Mobile Fest 2018, Kiev, Urkaine (Russian)](https://youtu.be/HrTz5jToQkk)

### Koin Developers Hub

* [Ready for Koin 2.0](https://medium.com/koin-developers/ready-for-koin-2-0-2722ab59cac3)
* [News from the trenches, What's next for Koin?](https://medium.com/koin-developers/news-from-the-trenches-whats-next-for-koin-994791d572d5)
* [Koin 1.0.0 Unleashed](https://medium.com/koin-developers/koin-1-0-0-unleashed-dcc15b293a3a)
* [Opening Koin 1.0.0 Beta](https://medium.com/koin-developers/opening-the-koin-1-0-0-beta-version-99cb8be1c308)
* [On the road to Koin 1.0](https://medium.com/koin-developers/on-the-road-to-koin-1-0-0-a624af55d07)
* [Koin 0.9.2 — Maintenance fixes, new branding, roadmap for 1.0.0 & some other nice announces](https://medium.com/koin-developers/koin-0-9-2-maintenance-fixes-new-branding-roadmap-for-1-0-0-some-other-nice-announces-94f14648e4ad)
* [Koin 0.9.1 - Bug fixes & Improvments](https://medium.com/koin-developers/koin-0-9-1-bug-fixes-improvements-bug-fixes-d257cd2766fa)
* [Koin 0.9.0 - Getting close to stable](https://medium.com/koin-developers/koin-0-9-0-getting-close-to-stable-release-74df9bb9e181)
* [Unlock your Android ViewModel power with Koin](https://medium.com/@giuliani.arnaud/unlock-your-android-viewmodel-power-with-koin-23eda8f493be)
* [koin 0.8.2 Improvements bugfixes and crash fix](https://medium.com/koin-developers/koin-0-8-2-improvements-bugfixes-and-crash-fix-6b6809fc1dd2)
* [Koin release 0.8.0](https://medium.com/koin-developers/koin-released-in-0-8-0-welcome-to-koin-spark-koin-android-architecture-f6270a7d4808)


## Contributors

This project exists thanks to all the people who contribute. [[Contribute](CONTRIBUTING.adoc)].
<a href="https://github.com/InsertKoinIO/koin/graphs/contributors"><img src="https://opencollective.com/koin/contributors.svg?width=890&button=false" /></a>


## OpenCollective

[![Backers on Open Collective](https://opencollective.com/koin/backers/badge.svg)](#backers)
[![Sponsors on Open Collective](https://opencollective.com/koin/sponsors/badge.svg)](#sponsors)

### Backers

Thank you to all our backers! 🙏 [[Become a backer](https://opencollective.com/koin#backer)]

<a href="https://opencollective.com/koin#backers" target="_blank"><img src="https://opencollective.com/koin/backers.svg?width=890"></a>


### Sponsors

Support this project by becoming a sponsor. Your logo will show up here with a link to your website. [[Become a sponsor](https://opencollective.com/koin#sponsor)]

<a href="https://opencollective.com/koin/sponsor/0/website" target="_blank"><img src="https://opencollective.com/koin/sponsor/0/avatar.svg"></a>
<a href="https://opencollective.com/koin/sponsor/1/website" target="_blank"><img src="https://opencollective.com/koin/sponsor/1/avatar.svg"></a>
<a href="https://opencollective.com/koin/sponsor/2/website" target="_blank"><img src="https://opencollective.com/koin/sponsor/2/avatar.svg"></a>
<a href="https://opencollective.com/koin/sponsor/3/website" target="_blank"><img src="https://opencollective.com/koin/sponsor/3/avatar.svg"></a>
<a href="https://opencollective.com/koin/sponsor/4/website" target="_blank"><img src="https://opencollective.com/koin/sponsor/4/avatar.svg"></a>
<a href="https://opencollective.com/koin/sponsor/5/website" target="_blank"><img src="https://opencollective.com/koin/sponsor/5/avatar.svg"></a>
<a href="https://opencollective.com/koin/sponsor/6/website" target="_blank"><img src="https://opencollective.com/koin/sponsor/6/avatar.svg"></a>
<a href="https://opencollective.com/koin/sponsor/7/website" target="_blank"><img src="https://opencollective.com/koin/sponsor/7/avatar.svg"></a>
<a href="https://opencollective.com/koin/sponsor/8/website" target="_blank"><img src="https://opencollective.com/koin/sponsor/8/avatar.svg"></a>
<a href="https://opencollective.com/koin/sponsor/9/website" target="_blank"><img src="https://opencollective.com/koin/sponsor/9/avatar.svg"></a>
