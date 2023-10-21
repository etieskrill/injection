package org.etieskrill.engine;

/**
 * Mark methods which are called every frame, or otherwise in relation to the framerate.
 * Currently just a reminder, that no new objects or the like should be created in these, since that would put
 * unnecessary strain on the system, even if just to assist the GC somewhat.
 */
//TODO implement things to help IDEs, such as checks for new or method contracts which would hint to such
//     reevaluate value of this after some time, and if good, make bigger, better, thiccer, EXPAND
public @interface EveryFrame {}
