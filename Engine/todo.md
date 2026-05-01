so, overarching dilemma; i want the engine to both abstract away enough stuff and have reasonable enough defaults that
one may just write a game as they would in e.g. pygame and everything just kind of clicks together, but on the other
hand, i want pretty much everything to be as configurable as the low-level apis allow, should the user desire it. these
wants are of course entirely juxtaposed, but the main issue i've faced so far is a user (who is not me with all the
knowledge of the engine, though that is highly unlikely to ever be the case) may not know which part of the api is meant
to be on a lower level than what they are currently using.

- [ ] rewrite render engine
    - [ ] get rid of that fuckass mesh/model matrix system, in any way pass just one valid model matrix to shader
    - [ ] camera viewport size should come from... the viewport being rendered into, duh
    - [ ] think hard about and adjust pipeline design, possibly after all the below
    - [ ] convert everything to pipelines
    - [ ] do *HARD* validation on pipeline config, especially including:
        - [ ] framebuffer bindings and states
        - [ ] texture bindings and uniform sets
    - [ ] redo shader ecosystem
        - [ ] define checks (on by default, figure out how to disable for release)
        - [ ] define material standard -> single/multiple materials per mesh
        - [ ] define default material uniform names
        - [ ] add default uniforms, e.g. time, framebuffer pixel size etc.
            - [ ] add as flags for engine shader constructor
            - [ ] detect usage for shader builders
            - [ ] set in renderer depending on feature set
    - [ ] auto detection and validation of vertex element layout and mapper
    - [ ] a shader should probably kinda come with a material instead of just loosely dangling about, right?
- [ ] figure out ecs services
    - [ ] probably auto add pretty much all services if relevant components are detected
    - [ ] do the service dependency graph
    - [ ] *perf only*: do caching / restructuring
    - [ ] develop various simple applications to find useful callbacks
- [ ] resource loading
    - [ ] cache *EVERYTHING*, see if there even is a case in which this is not the desired behaviour
- [ ] input system
    - [ ] add action system (yes, godot may have infected my subconscious a little bit)
    - [ ] action to input map, fallback and remapping api
    - [ ] make a saucy kotlin api
- [ ] reasonable 3d default view and input config
