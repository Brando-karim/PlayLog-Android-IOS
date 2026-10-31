package com.playlog.app.data

/**
 * Offline catalog used when no IGDB credentials are configured, and as a source of
 * instant results while the network search is in flight.
 */
object DemoCatalog {

    val games: List<Game> = listOf(
        Game(
            id = 900001, name = "Elden Ring", releaseYear = 2022,
            rating = 94.2, aggregatedRating = 96.0, ratingCount = 24310,
            genres = listOf("Role-playing (RPG)", "Adventure"),
            platforms = listOf("PC", "PlayStation 5", "Xbox Series X|S"),
            summary = "Rise, Tarnished, and be guided by grace to brandish the power of the Elden Ring in the Lands Between.",
        ),
        Game(
            id = 900002, name = "Baldur's Gate 3", releaseYear = 2023,
            rating = 96.1, aggregatedRating = 96.0, ratingCount = 19820,
            genres = listOf("Role-playing (RPG)", "Strategy", "Turn-based strategy (TBS)"),
            platforms = listOf("PC", "macOS", "PlayStation 5", "Xbox Series X|S"),
            summary = "Gather your party and return to the Forgotten Realms in a tale of fellowship and betrayal, sacrifice and survival.",
        ),
        Game(
            id = 900003, name = "Hades", releaseYear = 2020,
            rating = 92.4, aggregatedRating = 93.0, ratingCount = 15600,
            genres = listOf("Role-playing (RPG)", "Hack and slash/Beat 'em up", "Indie"),
            platforms = listOf("PC", "Nintendo Switch", "PlayStation 4", "Xbox One"),
            summary = "Defy the god of the dead as you hack and slash out of the Underworld in this rogue-like dungeon crawler.",
        ),
        Game(
            id = 900004, name = "The Legend of Zelda: Breath of the Wild", releaseYear = 2017,
            rating = 93.8, aggregatedRating = 97.0, ratingCount = 21400,
            genres = listOf("Adventure", "Role-playing (RPG)"),
            platforms = listOf("Nintendo Switch", "Wii U"),
            summary = "Step into a world of discovery, exploration, and adventure in a boundary-breaking new game in the Zelda series.",
        ),
        Game(
            id = 900005, name = "Red Dead Redemption 2", releaseYear = 2018,
            rating = 93.1, aggregatedRating = 97.0, ratingCount = 23110,
            genres = listOf("Shooter", "Adventure"),
            platforms = listOf("PC", "PlayStation 4", "Xbox One"),
            summary = "America, 1899. Arthur Morgan and the Van der Linde gang are outlaws on the run in the dying days of the Wild West.",
        ),
        Game(
            id = 900006, name = "Hollow Knight", releaseYear = 2017,
            rating = 91.7, aggregatedRating = 90.0, ratingCount = 17420,
            genres = listOf("Platform", "Adventure", "Indie"),
            platforms = listOf("PC", "Nintendo Switch", "PlayStation 4", "Xbox One"),
            summary = "Forge your own path in Hallownest, a vast ruined kingdom of insects and heroes, in this hand-drawn action adventure.",
        ),
        Game(
            id = 900007, name = "Celeste", releaseYear = 2018,
            rating = 90.6, aggregatedRating = 91.0, ratingCount = 12980,
            genres = listOf("Platform", "Indie"),
            platforms = listOf("PC", "Nintendo Switch", "PlayStation 4", "Xbox One"),
            summary = "Help Madeline survive her inner demons on her journey to the top of Celeste Mountain in this tight platformer.",
        ),
        Game(
            id = 900008, name = "God of War", releaseYear = 2018,
            rating = 92.9, aggregatedRating = 94.0, ratingCount = 20540,
            genres = listOf("Hack and slash/Beat 'em up", "Adventure"),
            platforms = listOf("PC", "PlayStation 4", "PlayStation 5"),
            summary = "Kratos returns, now a father, journeying through the Norse realms with his son Atreus.",
        ),
        Game(
            id = 900009, name = "Stardew Valley", releaseYear = 2016,
            rating = 90.2, aggregatedRating = 89.0, ratingCount = 16770,
            genres = listOf("Role-playing (RPG)", "Simulator", "Indie"),
            platforms = listOf("PC", "Nintendo Switch", "PlayStation 4", "Xbox One", "Mobile"),
            summary = "You inherited your grandfather's old farm plot in Stardew Valley. Build the farm of your dreams.",
        ),
        Game(
            id = 900010, name = "Portal 2", releaseYear = 2011,
            rating = 92.1, aggregatedRating = 94.0, ratingCount = 18900,
            genres = listOf("Puzzle", "Platform", "Shooter"),
            platforms = listOf("PC", "PlayStation 3", "Xbox 360"),
            summary = "The sequel to the acclaimed puzzle-platformer, featuring co-op and the return of GLaDOS.",
        ),
        Game(
            id = 900011, name = "Cyberpunk 2077", releaseYear = 2020,
            rating = 87.4, aggregatedRating = 86.0, ratingCount = 22330,
            genres = listOf("Role-playing (RPG)", "Shooter", "Adventure"),
            platforms = listOf("PC", "PlayStation 5", "Xbox Series X|S"),
            summary = "A open-world action-adventure RPG set in Night City, a megalopolis obsessed with power and body modification.",
        ),
        Game(
            id = 900012, name = "Sekiro: Shadows Die Twice", releaseYear = 2019,
            rating = 90.8, aggregatedRating = 91.0, ratingCount = 14320,
            genres = listOf("Adventure", "Role-playing (RPG)"),
            platforms = listOf("PC", "PlayStation 4", "Xbox One"),
            summary = "Explore late 1500s Sengoku Japan as a 'one-armed wolf', a disgraced and disfigured warrior.",
        ),
        Game(
            id = 900013, name = "Disco Elysium", releaseYear = 2019,
            rating = 91.3, aggregatedRating = 91.0, ratingCount = 11220,
            genres = listOf("Role-playing (RPG)", "Adventure", "Indie"),
            platforms = listOf("PC", "macOS", "PlayStation 4", "Nintendo Switch"),
            summary = "A groundbreaking detective RPG where you play a detective with a unique skill system and a shattered mind.",
        ),
        Game(
            id = 900014, name = "Doom Eternal", releaseYear = 2020,
            rating = 87.9, aggregatedRating = 88.0, ratingCount = 13540,
            genres = listOf("Shooter"),
            platforms = listOf("PC", "PlayStation 4", "Xbox One", "Nintendo Switch"),
            summary = "Hell's armies have invaded Earth. Become the Slayer and rip and tear until it is done.",
        ),
        Game(
            id = 900015, name = "Outer Wilds", releaseYear = 2019,
            rating = 89.4, aggregatedRating = 89.0, ratingCount = 8120,
            genres = listOf("Adventure", "Puzzle", "Indie"),
            platforms = listOf("PC", "Xbox One", "PlayStation 4", "Nintendo Switch"),
            summary = "A open world mystery about a solar system trapped in an endless time loop.",
        ),
        Game(
            id = 900016, name = "It Takes Two", releaseYear = 2021,
            rating = 88.7, aggregatedRating = 89.0, ratingCount = 9640,
            genres = listOf("Platform", "Adventure", "Puzzle"),
            platforms = listOf("PC", "PlayStation 4", "PlayStation 5", "Xbox Series X|S"),
            summary = "Embark on the craziest journey of your life in a co-op-only platform adventure for two players.",
        ),
    )

    fun search(query: String): List<Game> {
        if (query.isBlank()) return games
        val q = query.trim()
        return games.filter { it.name.contains(q, ignoreCase = true) }
    }
}
