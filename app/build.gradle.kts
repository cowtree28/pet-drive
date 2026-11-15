plugins {
    id("com.android.application")
}

android {
    namespace = "ai.petdrive"
    compileSdk = 35
    useLibrary("android.car") // 속도: CarPropertyManager (Vehicle SDK에 속도 API가 없음)

    defaultConfig {
        applicationId = "ai.petdrive"
        minSdk = 28
        targetSdk = 34
        versionCode = 2
        versionName = "0.2"
    }
    defaultConfig.ndk { abiFilters += listOf("x86_64", "arm64-v8a") } // 에뮬레이터(x86_64)와 차량(arm64)만

    buildTypes {
        release {
            // 메일 첨부 크기를 줄이려고 코드 축소. Pleos SDK는 AIDL·리플렉션을 써서 통째로 보존한다(proguard-rules.pro)
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("debug") // 공모전 데모용. 배포 때는 팀 키로 서명
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    // Pleos Connect SDK (공모전 5종 중 지금 쓰는 것)
    implementation("ai.pleos.playground:Vehicle:2.3.2")
    implementation("ai.pleos.playground:TextToSpeech:3.3.1")
    implementation("ai.pleos.playground:NaviHelper:3.0.5")
    implementation("ai.pleos.playground:SpeechToText:3.3.1")
}
