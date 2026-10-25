#version 150
in vec3 Position;
uniform mat4 FloorMat;
uniform mat4 FloorProjection;
out vec3 Local;
void main(){Local=Position;gl_Position=FloorProjection*FloorMat*vec4(Position,1.0);}
