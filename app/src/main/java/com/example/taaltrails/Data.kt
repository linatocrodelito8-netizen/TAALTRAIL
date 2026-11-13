package com.example.taaltrails

enum class Kind { PLACES, FOOD, ACTIVITIES }

data class Place(
    val id: String,
    val name: String,
    val kind: Kind,
    val tag: String,
    val desc: String,
    val rating: Double,
    val reviews: Int,
    val address: String,
    val window: String,
    val cost: Int,
    val emoji: String,
) {
    val price: String get() = if (cost == 0) "Free entry" else "₱$cost"
}

val PLACES = listOf(
    Place("basilica", "Basilica of St. Martin of Tours", Kind.PLACES, "Heritage Site",
        "The largest Catholic church in Asia, watching over Taal since 1856.",
        4.9, 2148, "Calle Cesario Villa, Taal", "6:00 AM – 8:00 AM", 0, "⛪"),
    Place("village", "Taal Heritage Village", Kind.PLACES, "Heritage District",
        "Cobblestone streets lined with Spanish-era ancestral houses.",
        4.8, 1876, "Calle Agoncillo, Taal", "7:00 AM – 10:00 AM", 0, "🏘️"),
    Place("casa", "Casa Villavicencio", Kind.PLACES, "House Museum",
        "A restored 19th century mansion with original family heirlooms.",
        4.7, 964, "Calle Gliceria Marella, Taal", "9:00 AM – 4:00 PM", 150, "🏛️"),
    Place("viewpoint", "Taal Lake Viewpoint", Kind.PLACES, "Scenic View",
        "An open ridge view over the lake and Volcano Island.",
        4.6, 1523, "Tagaytay–Taal Ridge Road", "5:00 AM – 7:00 PM", 0, "🏞️"),
    Place("tortuga", "Villa Tortuga", Kind.PLACES, "Heritage House",
        "A heritage house styled for period photography and tours.",
        4.5, 738, "Calle Feline Agoncillo, Taal", "8:00 AM – 5:00 PM", 200, "🏠"),
    Place("longganisa", "Taal Longganisa", Kind.FOOD, "Local Specialty",
        "Garlicky, slightly sweet pork sausage cured the Taal way.",
        4.9, 1104, "Taal Public Market", "7:00 AM – 7:00 PM", 120, "🌭"),
    Place("tulingan", "Sinaing na Tulingan", Kind.FOOD, "Home Cooking",
        "Bullet tuna slow-simmered with tamarind and banana leaves.",
        4.7, 842, "Calle Marella, Taal", "11:00 AM – 8:00 PM", 180, "🐟"),
    Place("tapang", "Tapang Taal", Kind.FOOD, "Cured Meat",
        "Sweet-cured pork, pan-fried and served with garlic rice.",
        4.6, 611, "Calle Gen. Luna, Taal", "6:00 AM – 9:00 PM", 160, "🥩"),
    Place("tawilis", "Fried Tawilis", Kind.FOOD, "Lake Catch",
        "Crispy freshwater sardine found only in Taal Lake.",
        4.6, 530, "Lakeside, Taal", "10:00 AM – 8:00 PM", 200, "🍤"),
    Place("panutsa", "Panutsa", Kind.FOOD, "Local Sweets",
        "Crunchy peanut brittle, a Batangas pasalubong favorite.",
        4.4, 402, "Taal Public Market", "8:00 AM – 6:00 PM", 80, "🥜"),
    Place("sunset", "Lakeside Sunset Walk", Kind.ACTIVITIES, "Outdoor",
        "A relaxed evening stroll with golden light over the lake.",
        4.7, 689, "Taal Lakeshore", "4:30 PM – 6:30 PM", 0, "🌅"),
    Place("walk", "Heritage Walking Tour", Kind.ACTIVITIES, "Guided Tour",
        "A local guide walks you through Taal's most storied streets.",
        4.8, 920, "Meet at the Basilica steps", "8:00 AM – 11:00 AM", 250, "🚶"),
    Place("boat", "Taal Lake Boat Ride", Kind.ACTIVITIES, "Adventure",
        "Cross the lake by banca for a close look at the volcano.",
        4.5, 455, "Talisay Lakeshore", "7:00 AM – 4:00 PM", 500, "🚤"),
    Place("balisong", "Balisong Workshop Visit", Kind.ACTIVITIES, "Craft",
        "Watch artisans forge Taal's famous butterfly knives.",
        4.6, 377, "Calle Marella, Taal", "8:00 AM – 5:00 PM", 0, "🔨"),
    Place("barong", "Barong Embroidery Workshop", Kind.ACTIVITIES, "Craft",
        "See hand embroidery on barong and piña cloth up close.",
        4.5, 298, "Calle Agoncillo, Taal", "9:00 AM – 4:00 PM", 300, "🧵"),
)

fun placeById(id: String): Place = PLACES.first { it.id == id }