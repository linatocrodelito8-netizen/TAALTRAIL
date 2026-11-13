package com.example.taaltrails

/** Offline backup answers, used when the AI cannot be reached. */
fun offlineReply(q: String, seed: Int): ChatMsg {
    val t = q.lowercase().trim()
    fun has(p: String) = Regex(p).containsMatchIn(t)
    fun pick(vararg o: String) = o[(seed / 2) % o.size]
    fun by(vararg ids: String) = ids.map { placeById(it) }
    fun top(src: List<Place>, n: Int) = src.sortedByDescending { it.rating }.take(n)
    val words = t.split(Regex("\\s+")).size

    if (words <= 4 && has("^(hi|hello|hey|kumusta|good (morning|afternoon|evening))"))
        return ChatMsg(false, pick(
            "Hello! Ready to explore Taal? Ask me where to eat, what to see, or how to plan your day.",
            "Hi there! I can suggest food, heritage sites, sunset spots and more.",
            "Kumusta! Tell me what you like, such as history, food or views."))

    if (has("thank|salamat|thx"))
        return ChatMsg(false, pick("You're welcome! Enjoy Taal!", "Walang anuman!", "Happy to help."))

    if (has("volcano|eruption|erupt|safe|safety|alert|phivolcs"))
        return ChatMsg(false,
            "Taal Volcano is active and conditions can change. Always check the latest PHIVOLCS advisory and alert level before you go. Lake and boat activities are the first to be limited when alerts rise.",
            by("village", "basilica", "longganisa"))

    if (has("how (do|can|to) (i |we )?(get|go|reach|travel|commute)|commute|transport|bus|jeepney|tricycle|from manila|getting there|how far"))
        return ChatMsg(false,
            "Taal is roughly 2 to 3 hours by road from Metro Manila, depending on traffic. Take a bus to Lemery or Batangas City, then a jeepney or tricycle into the town proper. Check current schedules before you go. The Map tab can open Google Maps for directions.")

    if (has("how long|how many days|how much time|enough time|overnight|stay"))
        return ChatMsg(false,
            "Half a day covers the Basilica, the Heritage Village and one local meal. A full day lets you add a workshop and the lakeside sunset.",
            by("basilica", "village", "sunset"))

    if (has("best time|best season|when (should|to) (i |we )?(go|visit)|weather|rain|dry season|typhoon"))
        return ChatMsg(false,
            "The dry season, roughly November to May, is the most comfortable. Go early for the Heritage Village and save the lakeside for late afternoon.",
            by("basilica", "village", "sunset"))

    if (has("budget|cost|how much|cheap|price|afford|peso|₱|expensive|free")) {
        val free = PLACES.filter { it.cost == 0 }
        val meals = PLACES.filter { it.kind == Kind.FOOD }.map { it.cost }
        return ChatMsg(false,
            "Taal is easy on the wallet. ${free.size} spots are free entry. A local dish costs about ₱${meals.min()} to ₱${meals.max()}. Add stops to My Trip and I'll total the estimate.",
            top(free, 3))
    }

    if (has("souvenir|pasalubong|gift|shopping|buy|take home|balisong|barong|panutsa"))
        return ChatMsg(false,
            "Classic Taal pasalubong: panutsa, balisong knives and barong embroidery. Buy knives only from licensed workshops, and pack them in checked luggage if you're flying.",
            by("panutsa", "balisong", "barong"))

    val hm = Regex("(\\d+)\\s*(hour|hr)").find(t)
    if (hm != null || has("half[- ]day|full[- ]day|day trip|itinerary|plan my|one day|1 day")) {
        val hours = hm?.groupValues?.get(1)?.toIntOrNull()
        val n = when {
            hours != null -> (hours / 1.3).toInt().coerceIn(1, 5)
            has("full[- ]day|one day|1 day") -> 5
            else -> 3
        }
        val wantsFood = has("food|eat|hungry|dish|lunch|dinner|breakfast")
        val wantsHeritage = has("church|old|heritage|history|museum|house")
        val wantsView = has("view|sunset|lake|scenic|photo")
        val picks = PLACES.sortedByDescending { p ->
            var s = p.rating
            if (wantsFood && p.kind == Kind.FOOD) s += 10
            if (wantsHeritage && (p.tag.contains("Heritage") || p.tag.contains("Museum"))) s += 10
            if (wantsView && (p.tag.contains("Scenic") || p.name.contains("Sunset") || p.name.contains("Lake"))) s += 10
            s
        }.take(n)
        return ChatMsg(false, "Here is what I'd do in the time you have. Tap + to add stops to your trip.", picks)
    }

    if (has("about taal|history|famous|known for|what is taal|tell me about|marcela|agoncillo"))
        return ChatMsg(false,
            "Taal is a heritage town in Batangas, known for its Spanish-era ancestral houses, the Basilica of St. Martin of Tours, and crafts like balisong knives and barong embroidery. It is also the hometown of Marcela Agoncillo, who sewed the first Philippine flag.",
            by("basilica", "village"))

    if (has("food|eat|hungry|dish|lunch|dinner|breakfast|merienda|snack|delicac|restaurant|try"))
        return ChatMsg(false, pick(
            "You can't leave Taal without trying these. They're our top-rated food stops.",
            "Hungry? Here are the dishes locals recommend most."),
            top(PLACES.filter { it.kind == Kind.FOOD }, 3))

    if (has("family|kids|children|elderly|lola|lolo|senior"))
        return ChatMsg(false,
            "For an easy outing, stick to short walks and shaded stops: the Heritage Village, a relaxed sunset by the lake, and a good meal in between.",
            by("village", "sunset", "longganisa"))

    if (has("romantic|date|couple|anniversary"))
        return ChatMsg(false,
            "For a romantic evening, catch the sunset by the lake, then enjoy dinner over local home cooking.",
            by("sunset", "viewpoint", "tulingan"))

    if (has("sunset|view|photo|instagram|scenic|lake|sunrise"))
        return ChatMsg(false,
            "For views, the lake ridge and the lakeside sunset walk are hard to beat. For photos, shoot the Basilica in soft morning light.",
            by("sunset", "viewpoint", "basilica"))

    if (has("church|old|heritage|museum|house|ancestral|spanish|basilica|casa|villa"))
        return ChatMsg(false,
            "For heritage, start with the Basilica, then walk the old streets and step inside a restored mansion.",
            top(PLACES.filter { it.kind == Kind.PLACES && (it.tag.contains("Heritage") || it.tag.contains("Museum")) }, 3))

    if (has("activit|adventure|craft|workshop|tour|what can i do|things to do|fun"))
        return ChatMsg(false,
            "Here are the best things to do besides sightseeing. Craft workshops are a Taal highlight.",
            top(PLACES.filter { it.kind == Kind.ACTIVITIES }, 3))

    return ChatMsg(false,
        "I'm not sure about that one yet. Ask me about food, heritage sites, sunsets, getting there, the best time to visit, budgets, souvenirs or safety.")
}