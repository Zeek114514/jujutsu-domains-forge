#version 150
uniform sampler2D Reflection;
uniform vec2 Resolution;
uniform float Time;
uniform float Opacity;
uniform float Radius;
uniform vec3 CameraLocal;
uniform vec3 ClashNormal;
uniform float ClashOffset;
uniform float Clash;
in vec3 Local;
out vec4 fragColor;
float hash(vec2 p){return fract(sin(dot(p,vec2(127.1,311.7)))*43758.5453);}
float noise(vec2 p){vec2 i=floor(p),f=fract(p);f=f*f*(3.0-2.0*f);return mix(mix(hash(i),hash(i+vec2(1,0)),f.x),mix(hash(i+vec2(0,1)),hash(i+vec2(1,1)),f.x),f.y);}
float fbm(vec2 p){float n=0.0,a=0.5;for(int i=0;i<5;i++){n+=noise(p)*a;p=mat2(1.7,1.2,-1.2,1.7)*p+vec2(5.4,9.2);a*=0.5;}return n;}
void main(){
    float distance=length(Local.xz);
    if(distance>Radius)discard;
    float edge=1.0-smoothstep(Radius-2.0,Radius,distance);
    float side=dot(Local,ClashNormal)+ClashOffset;
    float region=mix(1.0,smoothstep(-0.4,0.4,side),Clash);
    float alpha=Opacity*edge*region;
    if(alpha<0.001)discard;
    vec2 p=Local.xz*0.105;
    vec2 drift=vec2(Time*0.018,-Time*0.012);
    vec2 warp=vec2(fbm(p+drift),fbm(p+vec2(7,13)-drift));
    float clouds=fbm(p+warp*2.7+drift);
    clouds=mix(clouds,0.46,smoothstep(48.0,160.0,length(Local.xz-CameraLocal.xz)));
    float mist=smoothstep(0.22,0.72,clouds);
    vec3 base=mix(vec3(0.0015,0.012,0.017),vec3(0.008,0.48,0.50),mist);
    base+=pow(mist,4.0)*vec3(0.0,0.025,0.03);
    vec2 uv=gl_FragCoord.xy/Resolution;
    // A tiny drift preserves recognizable silhouettes while avoiding a static glass look.
    uv+=vec2(sin(Local.z*0.7+Time*0.6),cos(Local.x*0.6-Time*0.4))*0.00025;
    vec4 reflected=texture(Reflection,uv);
    float grazing=1.0-abs(normalize(CameraLocal-Local).y);
    float mirror=mix(0.70,0.92,grazing)*reflected.a;
    vec3 color=mix(base,reflected.rgb*vec3(0.88,1.0,1.0),mirror);
    fragColor=vec4(color,alpha);
}
