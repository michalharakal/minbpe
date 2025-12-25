rootProject.name = "kotlin-tokenizer-cli"

// Include the minbpe-kmp library as a composite build
includeBuild("../../minbpe-kmp") {
    dependencySubstitution {
        substitute(module("sk.ainet.core:library")).using(project(":library"))
    }
}