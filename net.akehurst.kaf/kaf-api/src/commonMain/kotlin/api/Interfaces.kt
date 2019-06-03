package net.akehurst.kaf.api

interface Service {

}

interface AFIdentifiable {
    val identity: String
}
interface Identifiable {
    val af:AFIdentifiable
}

interface AFActive : AFIdentifiable
interface Active {
    val af:AFActive
}

interface AFComponent : AFActive
interface Component

interface AFApplication : AFComponent
interface Application {
    val af: AFApplication
}