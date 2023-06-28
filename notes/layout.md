1. define outermost size -> probably screen size in the beginning
2. define local max size
3. pass to layout function
4. clamp preferred and minimum sizes of child elements
5. interpolate if in clamping range
6. pass size to and focus child
7. jump to step 2 unless innermost element

8. pass innermost sizes to parent
9. focus parent element
10. position child elements
11. jump to step 8 unless outermost element