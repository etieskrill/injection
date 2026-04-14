package io.github.etieskrill.games.circles

import org.joml.Vector3fc

interface Environment {
    fun getTemperature(position: Vector3fc): Float
    fun addHeatEnergy(position: Vector3fc, energy: Float)
    fun update(delta: Float)
}

interface VisEnvironment {

    /**
     * All receptacles with an [id] that require vis in some form must first request it using this method, so that the
     * vis can be split equally and proportionate to [strength], up to the [max] that the receptacle can receive based
     * on its remaining capacity. Receptacles must have a unique [id] even within the same circle.
     *
     * @param id a vis receiver's global id
     * @param type the type of vis to pull
     * @param position the position to pull from
     * @param strength how strongly the vis is pulled
     * @param max how much vis can be received at most
     */
    fun requestVis(id: Int, type: VisType, position: Vector3fc, strength: Float, max: Float)

    /**
     * Get the allotted amount of vis of a certain [type] for a vis receiver [id]. Vis of a specific [type] and [id]
     * must first be requested using [requestVis].
     *
     * @return the amount of vis that could be pulled from the environment
     */
    fun getVis(id: Int, type: VisType): Float

    /**
     * First refills a certain fraction of missing vis up to a certain chunk limit, then redistributes it amongst
     * neighbouring chunks if there is an imbalance.
     */
    //TODO request methods also need a timeframe, or do they just use the one from [update]?
    fun update(delta: Float)

}
