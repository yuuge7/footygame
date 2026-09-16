package com.example.footygame.data

import com.example.footygame.models.Opponent

object Opponents {
    val premierLeague = listOf(
        Opponent("Arsenal", 85),
        Opponent("Aston Villa", 80),
        Opponent("Bournemouth", 76),
        Opponent("Brentford", 77),
        Opponent("Brighton", 78),
        Opponent("Burnley", 72),
        Opponent("Chelsea", 83),
        Opponent("Crystal Palace", 78),
        Opponent("Everton", 76),
        Opponent("Fulham", 77),
        Opponent("Leeds United", 74),
        Opponent("Liverpool", 86),
        Opponent("Manchester City", 86),
        Opponent("Manchester United", 80),
        Opponent("Newcastle United", 81),
        Opponent("Nottingham Forest", 78),
        Opponent("Sunderland", 73),
        Opponent("Tottenham Hotspur", 80),
        Opponent("West Ham United", 76),
        Opponent("Wolves", 74),
    )

    object Europe {
        val elite = listOf(
            Opponent("Real Madrid", 87),
            Opponent("Barcelona", 86),
            Opponent("Bayern Munich", 86),
            Opponent("Manchester City", 86),
            Opponent("Liverpool", 86),
            Opponent("Paris Saint-Germain", 87),
            Opponent("Arsenal", 85),
            Opponent("Inter", 85),
        )
        val strong = listOf(
            Opponent("Atlético Madrid", 83),
            Opponent("Borussia Dortmund", 81),
            Opponent("Bayer Leverkusen", 81),
            Opponent("Juventus", 81),
            Opponent("AC Milan", 80),
            Opponent("Chelsea", 83),
            Opponent("Napoli", 82),
            Opponent("Benfica", 79),
            Opponent("Aston Villa", 80),
        )
        val mid = listOf(
            Opponent("Porto", 77),
            Opponent("Sporting CP", 78),
            Opponent("PSV Eindhoven", 77),
            Opponent("Feyenoord", 75),
            Opponent("Celtic", 73),
            Opponent("Club Brugge", 75),
            Opponent("Atalanta", 78),
            Opponent("Monaco", 76),
            Opponent("Lille", 75),
            Opponent("RB Leipzig", 78),
            Opponent("Galatasaray", 76),
            Opponent("Shakhtar Donetsk", 73),
        )
        val low = listOf(
            Opponent("Red Star Belgrade", 70),
            Opponent("Dinamo Zagreb", 71),
            Opponent("Young Boys", 70),
            Opponent("Sturm Graz", 68),
            Opponent("Slovan Bratislava", 66),
            Opponent("Qarabağ", 69),
            Opponent("Copenhagen", 71),
            Opponent("Olympiacos", 72),
            Opponent("Slavia Prague", 71),
            Opponent("Bodø/Glimt", 71),
            Opponent("Union Saint-Gilloise", 70),
            Opponent("Kairat Almaty", 66),
        )
    }

    object EuropaLeague {
        val strong = listOf(
            Opponent("Roma", 81),
            Opponent("Lazio", 80),
            Opponent("Villarreal", 80),
            Opponent("Athletic Club", 80),
            Opponent("Eintracht Frankfurt", 79),
            Opponent("Real Sociedad", 79),
            Opponent("Lyon", 79),
            Opponent("Ajax", 78),
            Opponent("Fenerbahçe", 78),
            Opponent("Porto", 78),
        )
        val mid = listOf(
            Opponent("Real Betis", 77),
            Opponent("Freiburg", 76),
            Opponent("Braga", 75),
            Opponent("Nice", 75),
            Opponent("Rangers", 74),
            Opponent("AZ Alkmaar", 74),
            Opponent("Anderlecht", 73),
            Opponent("Twente", 73),
            Opponent("Midtjylland", 72),
            Opponent("Ferencváros", 71),
        )
        val low = listOf(
            Opponent("Viktoria Plzeň", 70),
            Opponent("Ludogorets", 69),
            Opponent("FCSB", 69),
            Opponent("Malmö", 69),
            Opponent("Maccabi Tel Aviv", 68),
            Opponent("Elfsborg", 67),
            Opponent("Slovan Bratislava", 66),
            Opponent("RFS", 62),
        )
    }

    object ConferenceLeague {
        val strong = listOf(
            Opponent("Fiorentina", 78),
            Opponent("Legia Warsaw", 72),
            Opponent("Heidenheim", 72),
            Opponent("Copenhagen", 71),
            Opponent("Gent", 71),
            Opponent("Vitória de Guimarães", 71),
            Opponent("Rapid Wien", 70),
            Opponent("Djurgården", 70),
        )
        val mid = listOf(
            Opponent("Jagiellonia Białystok", 70),
            Opponent("Cercle Brugge", 70),
            Opponent("Molde", 69),
            Opponent("Lugano", 69),
            Opponent("Pafos", 68),
            Opponent("Omonia", 67),
            Opponent("APOEL", 67),
            Opponent("Celje", 66),
        )
        val low = listOf(
            Opponent("Mladá Boleslav", 64),
            Opponent("St. Gallen", 64),
            Opponent("Shamrock Rovers", 63),
            Opponent("Astana", 63),
            Opponent("HJK", 62),
            Opponent("Víkingur Reykjavík", 61),
            Opponent("Larne", 56),
            Opponent("The New Saints", 57),
        )
    }

    object Nations {
        val elite = listOf(
            Opponent("France", 88),
            Opponent("Brazil", 86),
            Opponent("Argentina", 87),
            Opponent("England", 87),
            Opponent("Spain", 88),
            Opponent("Germany", 85),
            Opponent("Portugal", 86),
        )
        val strong = listOf(
            Opponent("Netherlands", 84),
            Opponent("Belgium", 82),
            Opponent("Italy", 82),
            Opponent("Croatia", 81),
            Opponent("Uruguay", 81),
            Opponent("Colombia", 81),
            Opponent("Morocco", 82),
        )
        val mid = listOf(
            Opponent("USA", 77),
            Opponent("Mexico", 77),
            Opponent("Japan", 78),
            Opponent("Senegal", 78),
            Opponent("Switzerland", 78),
            Opponent("Denmark", 78),
            Opponent("Ecuador", 76),
            Opponent("South Korea", 76),
            Opponent("Norway", 78),
            Opponent("Austria", 77),
        )
        val low = listOf(
            Opponent("Australia", 73),
            Opponent("Canada", 74),
            Opponent("Iran", 73),
            Opponent("Saudi Arabia", 70),
            Opponent("Ghana", 72),
            Opponent("Qatar", 68),
            Opponent("New Zealand", 68),
            Opponent("Jordan", 69),
            Opponent("Uzbekistan", 70),
            Opponent("Cape Verde", 69),
            Opponent("Panama", 69),
            Opponent("Tunisia", 71),
        )
    }

    /** FA Cup opponents by tier; ratings come from the round, not the club. */
    object FaCup {
        val nonLeague = listOf(
            "Hashtag United", "Dulwich Hamlet", "Marine", "Chorley", "Scarborough Athletic",
            "Maidstone United", "Yeovil Town", "Woking", "Altrincham", "Boreham Wood",
            "Gateshead", "Southend United", "Hartlepool United", "York City", "Ebbsfleet United",
            "Aldershot Town", "Solihull Moors", "Torquay United", "Kidderminster Harriers",
            "FC United of Manchester",
        )
        val footballLeague = listOf(
            "Bradford City", "Stockport County", "Barnsley", "Bolton Wanderers",
            "Peterborough United", "Doncaster Rovers", "Port Vale", "Notts County", "MK Dons",
            "Crawley Town", "Swindon Town", "Salford City", "Exeter City", "Wigan Athletic",
        )
        val championship = listOf(
            "Leicester City", "Southampton", "Ipswich Town", "Sheffield United", "Middlesbrough",
            "Norwich City", "Coventry City", "Wrexham", "Birmingham City", "Stoke City",
            "Watford", "Derby County",
        )
        val premierLeague = listOf(
            "Brighton", "Brentford", "Fulham", "Crystal Palace", "Everton", "West Ham United",
            "Nottingham Forest", "Bournemouth", "Aston Villa", "Newcastle United",
            "Tottenham Hotspur", "Manchester United",
        )
        val giants = listOf("Arsenal", "Liverpool", "Manchester City", "Chelsea")
    }
}
