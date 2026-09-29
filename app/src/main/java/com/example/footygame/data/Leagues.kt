package com.example.footygame.data

import com.example.footygame.models.DraftMode
import com.example.footygame.models.League
import com.example.footygame.models.Opponent

/**
 * The leagues a player career can play in: today's clubs as fixtures (like the Premier League challenge
 * plays today's top flight), and which clubs in the squad data belong to each. Names match the squad data,
 * so a career club is kept out of its own fixtures.
 */
object Leagues {

    fun teams(league: League): List<Opponent> = when (league) {
        League.PREMIER_LEAGUE -> Opponents.premierLeague
        League.LA_LIGA -> laLiga
        League.SERIE_A -> serieA
        League.BUNDESLIGA -> bundesliga
        League.LIGUE_1 -> ligue1
        League.EREDIVISIE -> eredivisie
        League.PRIMEIRA_LIGA -> primeiraLiga
        League.SCOTTISH_PREMIERSHIP -> scotland
        League.SUPER_LIG -> superLig
        League.RUSSIAN_PREMIER_LEAGUE -> russia
        League.UKRAINIAN_PREMIER_LEAGUE -> ukraine
        League.GREEK_SUPER_LEAGUE -> greece
    }

    /** The league a club in the squad data plays in; null for clubs whose league isn't in the game. */
    fun leagueOf(club: String): League? = if (club in englishClubs) League.PREMIER_LEAGUE else foreignClubs[club]

    private val englishClubs: Set<String> by lazy { ClubSeasons.poolFor(DraftMode.EPL).mapTo(HashSet()) { it.club } }

    private val foreignClubs: Map<String, League> = mapOf(
        League.LA_LIGA to listOf(
            "Atlético Madrid", "Barcelona", "Celta Vigo", "Deportivo La Coruña", "Málaga", "Real Madrid",
            "Real Sociedad", "Sevilla", "Valencia", "Villarreal",
        ),
        League.SERIE_A to listOf("AC Milan", "Atalanta", "Fiorentina", "Inter", "Juventus", "Lazio", "Napoli", "Roma"),
        League.BUNDESLIGA to listOf(
            "Bayern Munich", "Borussia Dortmund", "Eintracht Frankfurt", "Borussia Mönchengladbach", "Kaiserslautern",
            "RB Leipzig", "Bayer Leverkusen", "Schalke 04", "Werder Bremen", "Wolfsburg",
        ),
        League.LIGUE_1 to listOf("Auxerre", "Bordeaux", "Lille", "Lyon", "Marseille", "Monaco", "Nantes", "Paris Saint-Germain"),
        League.EREDIVISIE to listOf("Ajax", "Feyenoord", "PSV Eindhoven"),
        League.PRIMEIRA_LIGA to listOf("Benfica", "Porto", "Sporting CP"),
        League.SCOTTISH_PREMIERSHIP to listOf("Celtic", "Rangers"),
        League.SUPER_LIG to listOf("Beşiktaş", "Fenerbahçe", "Galatasaray"),
        League.RUSSIAN_PREMIER_LEAGUE to listOf("CSKA Moscow", "Lokomotiv Moscow", "Spartak Moscow", "Zenit"),
        League.UKRAINIAN_PREMIER_LEAGUE to listOf("Dynamo Kyiv", "Shakhtar Donetsk"),
        League.GREEK_SUPER_LEAGUE to listOf("Olympiacos", "Panathinaikos"),
    ).flatMap { (league, clubs) -> clubs.map { it to league } }.toMap()

    private val laLiga = listOf(
        Opponent("Real Madrid", 87), Opponent("Barcelona", 86), Opponent("Atlético Madrid", 83),
        Opponent("Athletic Club", 79), Opponent("Villarreal", 79), Opponent("Real Betis", 78),
        Opponent("Real Sociedad", 77), Opponent("Celta Vigo", 75), Opponent("Sevilla", 75),
        Opponent("Girona", 75), Opponent("Osasuna", 74), Opponent("Rayo Vallecano", 74),
        Opponent("Valencia", 74), Opponent("Mallorca", 73), Opponent("Espanyol", 73),
        Opponent("Getafe", 73), Opponent("Alavés", 72), Opponent("Elche", 71),
        Opponent("Levante", 71), Opponent("Real Oviedo", 70),
    )

    private val serieA = listOf(
        Opponent("Inter", 85), Opponent("Napoli", 83), Opponent("Juventus", 81),
        Opponent("AC Milan", 81), Opponent("Atalanta", 79), Opponent("Roma", 79),
        Opponent("Lazio", 78), Opponent("Como", 77), Opponent("Fiorentina", 77),
        Opponent("Bologna", 77), Opponent("Torino", 74), Opponent("Udinese", 73),
        Opponent("Genoa", 72), Opponent("Parma", 72), Opponent("Cagliari", 72),
        Opponent("Sassuolo", 72), Opponent("Lecce", 71), Opponent("Hellas Verona", 71),
        Opponent("Cremonese", 70), Opponent("Pisa", 70),
    )

    private val bundesliga = listOf(
        Opponent("Bayern Munich", 87), Opponent("Bayer Leverkusen", 81), Opponent("Borussia Dortmund", 81),
        Opponent("RB Leipzig", 80), Opponent("Eintracht Frankfurt", 78), Opponent("VfB Stuttgart", 78),
        Opponent("SC Freiburg", 76), Opponent("Borussia Mönchengladbach", 75), Opponent("Wolfsburg", 75),
        Opponent("Mainz 05", 75), Opponent("Hoffenheim", 75), Opponent("Werder Bremen", 74),
        Opponent("Union Berlin", 74), Opponent("Augsburg", 73), Opponent("Hamburger SV", 72),
        Opponent("1. FC Köln", 72), Opponent("St. Pauli", 71), Opponent("Heidenheim", 70),
    )

    private val ligue1 = listOf(
        Opponent("Paris Saint-Germain", 87), Opponent("Marseille", 79), Opponent("Monaco", 79),
        Opponent("Lille", 77), Opponent("Lyon", 77), Opponent("Nice", 76),
        Opponent("Lens", 76), Opponent("Strasbourg", 75), Opponent("Rennes", 75),
        Opponent("Brest", 73), Opponent("Toulouse", 73), Opponent("Nantes", 72),
        Opponent("Auxerre", 71), Opponent("Angers", 70), Opponent("Le Havre", 70),
        Opponent("Lorient", 70), Opponent("Paris FC", 70), Opponent("Metz", 69),
    )

    private val eredivisie = listOf(
        Opponent("PSV Eindhoven", 78), Opponent("Feyenoord", 77), Opponent("Ajax", 76),
        Opponent("AZ Alkmaar", 74), Opponent("FC Twente", 72), Opponent("FC Utrecht", 71),
        Opponent("NEC Nijmegen", 70), Opponent("Go Ahead Eagles", 69), Opponent("Sparta Rotterdam", 68),
        Opponent("Heerenveen", 68), Opponent("Groningen", 67), Opponent("Fortuna Sittard", 67),
        Opponent("PEC Zwolle", 66), Opponent("NAC Breda", 66), Opponent("Heracles Almelo", 66),
        Opponent("Excelsior", 65), Opponent("Telstar", 64), Opponent("FC Volendam", 64),
    )

    private val primeiraLiga = listOf(
        Opponent("Benfica", 80), Opponent("Sporting CP", 80), Opponent("Porto", 79),
        Opponent("Braga", 75), Opponent("Vitória de Guimarães", 72), Opponent("Famalicão", 70),
        Opponent("Gil Vicente", 69), Opponent("Moreirense", 68), Opponent("Casa Pia", 68),
        Opponent("Estoril", 68), Opponent("Rio Ave", 68), Opponent("Santa Clara", 68),
        Opponent("Arouca", 67), Opponent("Nacional", 66), Opponent("Estrela da Amadora", 66),
        Opponent("Tondela", 65), Opponent("Alverca", 65), Opponent("AVS", 64),
    )

    private val scotland = listOf(
        Opponent("Celtic", 75), Opponent("Rangers", 73), Opponent("Hearts", 67),
        Opponent("Aberdeen", 66), Opponent("Hibernian", 66), Opponent("Motherwell", 64),
        Opponent("Dundee United", 64), Opponent("St Mirren", 63), Opponent("Kilmarnock", 63),
        Opponent("Dundee", 62), Opponent("Falkirk", 62), Opponent("Livingston", 61),
    )

    private val superLig = listOf(
        Opponent("Galatasaray", 79), Opponent("Fenerbahçe", 78), Opponent("Beşiktaş", 75),
        Opponent("Trabzonspor", 73), Opponent("Başakşehir", 72), Opponent("Samsunspor", 71),
        Opponent("Göztepe", 70), Opponent("Kasımpaşa", 69), Opponent("Eyüpspor", 69),
        Opponent("Alanyaspor", 69), Opponent("Antalyaspor", 68), Opponent("Konyaspor", 68),
        Opponent("Rizespor", 68), Opponent("Gaziantep", 68), Opponent("Kayserispor", 67),
        Opponent("Gençlerbirliği", 66), Opponent("Kocaelispor", 66), Opponent("Karagümrük", 66),
    )

    private val russia = listOf(
        Opponent("Zenit", 77), Opponent("Krasnodar", 75), Opponent("Spartak Moscow", 74),
        Opponent("CSKA Moscow", 74), Opponent("Lokomotiv Moscow", 73), Opponent("Dynamo Moscow", 73),
        Opponent("Rostov", 70), Opponent("Rubin Kazan", 69), Opponent("Akhmat Grozny", 68),
        Opponent("Krylia Sovetov", 68), Opponent("Baltika", 68), Opponent("Orenburg", 66),
        Opponent("Dynamo Makhachkala", 66), Opponent("Nizhny Novgorod", 65), Opponent("Akron Tolyatti", 65),
        Opponent("Sochi", 65),
    )

    private val ukraine = listOf(
        Opponent("Shakhtar Donetsk", 74), Opponent("Dynamo Kyiv", 74), Opponent("Polissya Zhytomyr", 67),
        Opponent("Kryvbas", 66), Opponent("Oleksandriya", 66), Opponent("Zorya Luhansk", 65),
        Opponent("Karpaty Lviv", 65), Opponent("LNZ Cherkasy", 64), Opponent("Veres Rivne", 64),
        Opponent("Kolos Kovalivka", 64), Opponent("Rukh Lviv", 63), Opponent("Obolon Kyiv", 63),
        Opponent("Metalist 1925", 63), Opponent("Kudrivka", 62), Opponent("Epitsentr", 62),
        Opponent("Poltava", 61),
    )

    private val greece = listOf(
        Opponent("Olympiacos", 76), Opponent("PAOK", 75), Opponent("AEK Athens", 75),
        Opponent("Panathinaikos", 74), Opponent("Aris", 69), Opponent("OFI", 67),
        Opponent("Volos", 66), Opponent("Asteras Tripolis", 66), Opponent("Atromitos", 66),
        Opponent("Levadiakos", 65), Opponent("Panetolikos", 65), Opponent("Kifisia", 64),
        Opponent("AEL", 63), Opponent("Panserraikos", 63),
    )
}
