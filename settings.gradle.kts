pluginManagement {
    repositories {
        //maven { url = uri("https://maven.aliyun.com/repository/google") }
        //maven { url = uri("https://maven.aliyun.com/repository/jcenter") }
        //maven { url = uri("https://maven.aliyun.com/repository/public") }
        //gradlePluginPortal()
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        gradlePluginPortal()
        mavenCentral()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "My Application"
include(":app")
