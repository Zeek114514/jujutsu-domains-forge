#version 150
uniform vec2 Resolution;
uniform float Time;
uniform float DomainStyle;
uniform float Opacity;
uniform float TanHalfFov;
uniform vec3 CameraRight;
uniform vec3 CameraUp;
uniform vec3 CameraForward;
uniform vec3 VoidAxis;
uniform float DomainAge;
uniform float ShrineSky;
uniform float VoidOpacity;
uniform float ShrineOpacity;
uniform float DomainRadius;
uniform float ClashAmount;
uniform float Closing;
uniform float Overlay;
uniform float Veil;
uniform vec3 CameraOffset;
uniform vec3 ClashNormal;
out vec4 fragColor;

const float PI = 3.14159265359;

float hash(vec3 p) {
    p = fract(p * 0.3183099 + vec3(0.11, 0.23, 0.37));
    p *= 17.0;
    return fract(p.x * p.y * p.z * (p.x + p.y + p.z));
}
float noise(vec3 p) {
    vec3 cell = floor(p), f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    return mix(mix(mix(hash(cell), hash(cell + vec3(1,0,0)), f.x),
                   mix(hash(cell + vec3(0,1,0)), hash(cell + vec3(1,1,0)), f.x), f.y),
               mix(mix(hash(cell + vec3(0,0,1)), hash(cell + vec3(1,0,1)), f.x),
                   mix(hash(cell + vec3(0,1,1)), hash(cell + vec3(1,1,1)), f.x), f.y), f.z);
}
float fbm(vec3 p) {
    float sum = 0.0, amplitude = 0.5;
    for (int i = 0; i < 5; i++) {
        sum += amplitude * noise(p);
        p = p * 2.03 + vec3(5.2, 1.3, 7.1);
        amplitude *= 0.5;
    }
    return sum;
}
vec3 stars(vec3 rd) {
    vec3 result = vec3(0);
    // Sparse 3D star cells avoid a visible seam or poles in the sky.
    for (int layer = 0; layer < 3; layer++) {
        float scale = 110.0 + 95.0 * float(layer);
        vec3 p = rd * scale, cell = floor(p);
        float seed = hash(cell + float(layer) * 19.0);
        vec3 center = vec3(hash(cell + 9.1), hash(cell + 27.3), hash(cell + 63.7));
        float dist = length(fract(p) - center);
        float point = 1.0 - smoothstep(0.015, 0.12, dist);
        float twinkle = 0.82 + 0.18 * sin(Time * 0.7 + seed * 200.0);
        result += point * step(0.82, seed) * twinkle * mix(vec3(0.40,0.61,1.0),vec3(1.0,0.92,0.85),seed);
    }
    return result;
}
float ease(float x){x=clamp(x,0.0,1.0);return x*x*(3.0-2.0*x);}
vec3 redSky(vec3 rd){
    float clouds=fbm(rd*5.0+vec3(Time*0.02,0,Time*0.01));
    float storm=smoothstep(0.18,0.76,clouds);
    vec3 c=mix(vec3(0.012,0.001,0.006),vec3(0.65,0.015,0.048),storm);
    return c+vec3(0.11,0.008,0.012)*pow(max(0.0,1.0-abs(rd.y)*1.6),3.0);
}
void main() {
    vec2 uv = (2.0 * gl_FragCoord.xy - Resolution) / Resolution.y;
    vec3 rd = normalize(CameraForward + TanHalfFov * (uv.x * CameraRight + uv.y * CameraUp));
    vec3 axis = normalize(VoidAxis);
    vec3 side = normalize(cross(axis, abs(axis.y)>0.98?vec3(0,0,1):vec3(0,1,0)));
    vec3 top = normalize(cross(side, axis));
    float facing = dot(rd, axis);
    float theta = acos(clamp(facing, -1.0, 1.0));
    float angle = atan(dot(rd, top), dot(rd, side));
    float t = Time;

    // A diagonal, slowly evolving nebula surrounds the entire camera.
    float band = dot(rd, normalize(vec3(0.42,0.81,0.39)));
    float nebula = exp(-pow((band + 0.08 * sin(rd.x * 6.0)) * 5.5, 2.0));
    float cloud = fbm(rd * 7.0 + vec3(0.0, t * 0.008, 0.0));
    float detail = fbm(rd * 21.0 + cloud * 2.0);
    vec3 color = vec3(0.0015,0.002,0.006);
    color += nebula * pow(cloud, 2.7) * mix(vec3(0.15,0.045,0.30),vec3(0.16,0.27,0.49),detail) * 1.6;
    color += pow(detail, 8.0) * nebula * vec3(0.20,0.28,0.48);
    color += stars(rd);

    if(Overlay>0.5){fragColor=vec4(1.0,1.0,1.0,Veil*Opacity);return;}
    if(DomainStyle>0.5){
        fragColor=vec4(redSky(rd),ShrineSky*Opacity);return;
    }
    // Radial information filaments remain fixed in world direction.
    float lanes = 940.0;
    float lane = floor((angle + PI) / (2.0 * PI) * lanes);
    float laneSeed = hash(vec3(lane, 4, 8));
    float thin = pow(max(0.0, 1.0 - abs(fract((angle + PI) / (2.0 * PI) * lanes) - 0.5) * 2.0), 18.0);
    float pulse = pow(max(0.0, sin(theta * (12.0 + laneSeed * 24.0) - t * (0.5 + laneSeed) + laneSeed * 90.0)), 14.0);
    color += thin * step(0.63,laneSeed) * pulse * smoothstep(0.25,0.65,theta)
           * (1.0-smoothstep(1.5,2.7,theta)) * vec3(0.20,0.27,0.40);

    // Finite world-space plane: its center is anchored to the original domain,
    // so walking changes its projection instead of carrying it with the camera.
    vec3 haloCenter=axis*(DomainRadius*0.68)+vec3(0,DomainRadius*0.15,0);
    float denominator=dot(rd,axis);
    float safeDenominator=abs(denominator)<0.00001?(denominator<0.0?-0.00001:0.00001):denominator;
    float haloDistance=dot(haloCenter-CameraOffset,axis)/safeDenominator;
    vec3 haloOffset=CameraOffset+rd*haloDistance-haloCenter;
    float r=haloDistance>0.0?length(haloOffset)/(DomainRadius*0.29):10000.0;
    float ringAngle=atan(dot(haloOffset,top)+0.0000001,dot(haloOffset,side)+0.0000001);
    float ring = 0.0;
    float filaments = 0.0;
    for (int i = 0; i < 7; i++) {
        float layer = float(i);
        float phase = ringAngle + t * (0.16 + layer * 0.017);
        float center = 1.03 + 0.055 * layer + 0.075 * sin(phase * 3.0 + layer * 1.8)
                     + 0.028 * sin(phase * 7.0 - layer * 2.1);
        float offset = r - center;
        float wave = 0.5 + 0.5 * sin(phase * (11.0 + layer * 2.0) - r * 29.0 + layer * 4.0);
        float turbulence = noise(vec3(cos(phase)*9.0,sin(phase)*9.0,r*24.0+layer));
        float ribbon = exp(-offset*offset/(0.005 + layer*0.0007));
        ring += ribbon * (0.22 + 0.65 * wave) * (0.4 + turbulence);
        filaments += exp(-pow(offset + (turbulence-0.5)*0.045,2.0)/0.00012)
                    * (0.35 + wave*0.65);
    }
    float halo = exp(-pow((r-1.20)/0.31,2.0));
    float inner = smoothstep(0.88,0.99,r);
    color *= smoothstep(0.91,1.01,r);
    color += inner * (ring * vec3(0.25,0.32,0.48) + filaments * vec3(0.36,0.43,0.55));
    color += halo * inner * vec3(0.16,0.20,0.28);
    // White central halo, with flowing silver ribbons around it.
    color += exp(-pow((r-1.12)/0.13,2.0))*vec3(1.10,1.16,1.25)*0.7;
    color += exp(-r*r/0.032)*vec3(1.0,1.0,1.0)*0.38;
    // An unbroken, thin lens at the edge holds the silhouette together.
    float lensRadius = 1.0 + 0.009 * sin(ringAngle*3.0+t*0.2);
    float lens = exp(-pow((r-lensRadius)/0.008,2.0));
    color += lens * vec3(0.90,0.83,0.68) * 0.65;

    color=vec3(1.0)-exp(-color*1.25);
    color=pow(max(color,vec3(0)),vec3(0.80));

    // Both fronts start behind the caster and move through a fixed world sphere.
    // Looking backwards or sideways changes the visible sweep, never its direction.
    vec3 sweepAxis=normalize(vec3(VoidAxis.x,0.00001,VoidAxis.z));
    float b=dot(CameraOffset,rd);
    float rayEnd=-b+sqrt(max(0.0,b*b-dot(CameraOffset,CameraOffset)+DomainRadius*DomainRadius));
    vec3 endpoint=CameraOffset+rd*rayEnd;
    float projection=dot(endpoint,sweepAxis)/DomainRadius;
    float whiteFront=mix(-1.2,1.2,ease((DomainAge-147.0)/45.0));
    float spaceFront=mix(-1.2,1.2,ease((DomainAge-197.0)/50.0));
    float white=1.0-smoothstep(whiteFront-0.035,whiteFront+0.035,projection);
    float space=1.0-smoothstep(spaceFront-0.05,spaceFront+0.05,projection);
    vec3 domainColor=mix(vec3(1.0),color,space);

    // Equal-refinement clash: the sky separates along the line between casters.
    float clashSide=dot(rd,normalize(ClashNormal));
    if(Closing>=0.0)domainColor=mix(domainColor,vec3(1.0),ease(Closing/10.0)*(1.0-ease((Closing-10.0)/30.0)));
    float redHalf=mix(smoothstep(-0.025,0.025,clashSide),1.0,1.0-VoidOpacity)*ClashAmount*ShrineOpacity;
    domainColor=mix(domainColor,redSky(rd),redHalf);
    float seam=exp(-pow(clashSide/0.008,2.0))*ClashAmount*ShrineOpacity;
    domainColor+=seam*(0.4+0.2*sin(Time*4.0+rd.y*28.0))*vec3(0.65,0.20,0.30);
    fragColor=vec4(domainColor,white*Opacity);
}
