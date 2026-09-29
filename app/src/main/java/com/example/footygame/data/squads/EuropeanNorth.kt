package com.example.footygame.data.squads

import com.example.footygame.models.ClubSeason
import com.example.footygame.models.Player
import com.example.footygame.models.Pool

private fun european(kit: Kit, year: Int, vararg players: Player) = squad(kit, year, Pool.EUROPEAN_CLUB, *players)

/** Club seasons from Central Europe, the Balkans and Scandinavia. Ratings are game ratings for that season, not official figures. */
internal object EuropeanNorth {
    val all: List<ClubSeason> = listOf(
        european(Kits.LEGIA, 1995,
            gk("Maciej Szczęsny", 76),
            def("Zbigniew Mandziejewicz", 72), def("Marek Jóźwiak", 74), def("Jacek Bednarz", 72),
            def("Jacek Zieliński", 75), def("Krzysztof Ratajczyk", 71),
            mid("Grzegorz Lewandowski", 72), mid("Leszek Pisz", 77), mid("Radosław Michalski", 74),
            mid("Ryszard Staniek", 73), mid("Tomasz Wieszczycki", 73), mid("Adam Fedoruk", 70),
            att("Jerzy Podbrożny", 76), att("Cezary Kucharski", 74), att("Andrzej Kubica", 71),
        ),
        european(Kits.ROSENBORG, 1996,
            gk("Jørn Jamtfall", 74),
            def("Vegard Heggem", 77), def("Bjørn Otto Bragstad", 75), def("Erik Hoftun", 75),
            def("Bjørn Tore Kvarme", 75), def("Ståle Stensaas", 74), def("Karl Petter Løken", 73),
            def("Jon Olav Hjelde", 72),
            mid("Bent Skammelsrud", 77), mid("Roar Strand", 76), mid("Trond Egil Soltvedt", 75),
            mid("Fredrik Winsnes", 73), mid("Tom Kåre Staurvik", 70),
            att("Harald Brattbakk", 79), att("Steffen Iversen", 77), att("Mini Jakobsen", 76),
        ),
        european(Kits.SPARTA_PRAGUE, 2003,
            gk("Jaromír Blažek", 77),
            def("Petr Johana", 72), def("Pavel Pergl", 73), def("Martin Petráš", 72), def("Vladimír Labant", 72),
            def("Jiří Homola", 70),
            mid("Karel Poborský", 80), mid("Tomáš Hübschman", 74), mid("Radoslav Kováč", 74),
            mid("Rastislav Michalík", 71), mid("Libor Sionko", 75), mid("Lukáš Zelenka", 73), mid("Patrik Ježek", 72),
            att("Tomáš Jun", 75), att("Marek Kincl", 71), att("Igor Gluščević", 71),
        ),
        european(Kits.COPENHAGEN, 2010,
            gk("Johan Wiland", 74),
            def("Zdeněk Pospěch", 73), def("Mathias Jørgensen", 71), def("Mikael Antonsson", 72), def("Oscar Wendt", 72),
            def("Sölvi Ottesen", 69),
            mid("Christian Bolaños", 73), mid("William Kvist", 76), mid("Claudemir", 74), mid("Jesper Grønkjær", 74),
            mid("Martin Vingaard", 72), mid("Thomas Kristensen", 70), mid("Thomas Delaney", 68),
            mid("Hjalte Nørregaard", 68),
            att("Dame N'Doye", 76), att("César Santin", 73), att("Kenneth Zohore", 67),
        ),
        european(Kits.COPENHAGEN, 2023,
            gk("Kamil Grabara", 78),
            def("Kevin Diks", 76), def("Denis Vavro", 75), def("Elias Jelert", 72), def("Peter Ankersen", 71),
            def("Christian Sørensen", 71), def("Birger Meling", 72),
            mid("Lukas Lerager", 76), mid("Rasmus Falk", 74), mid("Diogo Gonçalves", 75), mid("Viktor Claesson", 76),
            mid("Mohamed Elyounoussi", 76), mid("Elias Achouri", 73), mid("Magnus Mattsson", 73),
            mid("Roony Bardghji", 74),
            att("Orri Óskarsson", 71), att("Jordan Larsson", 73), att("Andreas Cornelius", 73),
        ),
    )
}
