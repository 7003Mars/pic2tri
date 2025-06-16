# PicToTri
## To use the v7 version of the mod, check all releases

**A java "port" of [geometrize](https://github.com/Tw1ddle/geometrize-haxe), made for Mindustry.**

It turns images into mlog instructions and supports huge images

To render the image after placing the schematic:
- Place a logic processor and link it to all displays
- Link the processor to a button
- Paste the following code:
```
sensor e switch1 @enabled
jump 0 equal e 0
set i 0
getlink b i
draw clear 0 0 0 0 0 0
drawflush b
control enabled b 0 0 0 0
op add i i 1
set t @tick
jump 9 equal t @tick
jump 3 lessThan i @links
```
- Click the button