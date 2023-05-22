api which allows for rebinding of actions via key names as well as scancodes for edge cases
event chain input is always bound to one window
but there are multiple ways/devices to generate inputs -> api must handle all registered input methods
actions have to be defined per-scene/per-context -> differentiate scenes through multiple apis / store scene state in api

supported glfw input methods:
- mouse
- keyboard
- text
- joystick
- gamepad