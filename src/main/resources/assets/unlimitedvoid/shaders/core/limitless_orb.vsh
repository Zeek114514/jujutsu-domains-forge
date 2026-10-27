#version 150
in vec3 Position;
uniform mat4 OrbMat;
uniform mat4 OrbProjection;
uniform vec3 Center;
out vec3 Ray;
out vec3 Plane;
void main(){
    Ray=Position;
    Plane=Position-Center;
    gl_Position=OrbProjection*OrbMat*vec4(Position,1.0);
}
