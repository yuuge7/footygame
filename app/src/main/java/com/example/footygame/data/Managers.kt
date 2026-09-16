package com.example.footygame.data

import com.example.footygame.data.squads.slug
import com.example.footygame.models.DraftMode
import com.example.footygame.models.Manager
import com.example.footygame.models.ManagerTrait

/** Gaffers a finished XI can appoint, grouped by the kind of job they held. */
object Managers {
    private fun manager(name: String, trait: ManagerTrait) = Manager(slug(name), name, trait)

    val england = listOf(
        manager("Sir Alex Ferguson", ManagerTrait.BIG_GAMES),
        manager("Arsène Wenger", ManagerTrait.MAN_MANAGER),
        manager("José Mourinho", ManagerTrait.DEFENSIVE),
        manager("Pep Guardiola", ManagerTrait.TACTICIAN),
        manager("Jürgen Klopp", ManagerTrait.ATTACKING),
        manager("Carlo Ancelotti", ManagerTrait.BIG_GAMES),
        manager("Claudio Ranieri", ManagerTrait.MAN_MANAGER),
        manager("Antonio Conte", ManagerTrait.DEFENSIVE),
        manager("Rafael Benítez", ManagerTrait.TACTICIAN),
        manager("Kevin Keegan", ManagerTrait.ATTACKING),
        manager("Kenny Dalglish", ManagerTrait.MAN_MANAGER),
        manager("Harry Redknapp", ManagerTrait.MAN_MANAGER),
        manager("Mauricio Pochettino", ManagerTrait.TACTICIAN),
        manager("Mikel Arteta", ManagerTrait.TACTICIAN),
        manager("Arne Slot", ManagerTrait.ATTACKING),
        manager("Unai Emery", ManagerTrait.BIG_GAMES),
        manager("David Moyes", ManagerTrait.DEFENSIVE),
        manager("Roberto Mancini", ManagerTrait.DEFENSIVE),
    )

    val europe = listOf(
        manager("Zinedine Zidane", ManagerTrait.BIG_GAMES),
        manager("Diego Simeone", ManagerTrait.DEFENSIVE),
        manager("Marcello Lippi", ManagerTrait.TACTICIAN),
        manager("Jupp Heynckes", ManagerTrait.BIG_GAMES),
        manager("Luis Enrique", ManagerTrait.ATTACKING),
        manager("Fabio Capello", ManagerTrait.DEFENSIVE),
        manager("Louis van Gaal", ManagerTrait.TACTICIAN),
        manager("Xabi Alonso", ManagerTrait.ATTACKING),
        manager("Ottmar Hitzfeld", ManagerTrait.BIG_GAMES),
    )

    val nations = listOf(
        manager("Didier Deschamps", ManagerTrait.DEFENSIVE),
        manager("Vicente del Bosque", ManagerTrait.MAN_MANAGER),
        manager("Joachim Löw", ManagerTrait.TACTICIAN),
        manager("Marcello Lippi", ManagerTrait.TACTICIAN),
        manager("Lionel Scaloni", ManagerTrait.MAN_MANAGER),
        manager("Luiz Felipe Scolari", ManagerTrait.BIG_GAMES),
        manager("Zlatko Dalić", ManagerTrait.MAN_MANAGER),
        manager("Gareth Southgate", ManagerTrait.DEFENSIVE),
        manager("Aimé Jacquet", ManagerTrait.DEFENSIVE),
        manager("Louis van Gaal", ManagerTrait.TACTICIAN),
        manager("Óscar Tabárez", ManagerTrait.DEFENSIVE),
        manager("Roberto Martínez", ManagerTrait.ATTACKING),
        manager("Walid Regragui", ManagerTrait.DEFENSIVE),
        manager("Carlos Alberto Parreira", ManagerTrait.ATTACKING),
    )

    fun forMode(mode: DraftMode): List<Manager> = when (mode) {
        DraftMode.EPL, DraftMode.FAC -> england
        DraftMode.UCL -> england + europe
        DraftMode.WC -> nations
    }
}
