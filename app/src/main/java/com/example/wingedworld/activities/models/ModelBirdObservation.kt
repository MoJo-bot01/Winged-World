package com.example.wingedworld.activities.models

class ModelBirdObservation {

    var id:String = ""
    var uid:String =""
    var profileImage:String =""
    var name:String =""
    var date:String = ""
    var count:Int? = 0
    var timestamp:Long = 0
    var latitude:Double? = 0.0
    var longitude:Double? = 0.0
    var location:String = ""

    constructor()
    constructor(
        id: String,
        uid: String,
        profileImage:String,
        name:String,
        date:String,
        count:Int?,
        timestamp: Long,
        latitude: Double?,
        longitude: Double?,
        location:String
    ) {
        this.id = id
        this.uid = uid
        this.profileImage = profileImage
        this.name = name
        this.date = date
        this.count = count
        this.timestamp = timestamp
        this.latitude = latitude
        this.longitude = longitude
        this.location = location
    }
}