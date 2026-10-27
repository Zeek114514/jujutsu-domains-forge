#version 150
uniform sampler2D Scene;
uniform sampler2D WorldDepth;
uniform mat4 OrbMat;
uniform mat4 OrbProjection;
uniform vec3 Center;
uniform vec3 Right;
uniform vec3 Up;
uniform vec3 Axis;
uniform vec2 Resolution;
uniform vec2 Viewport;
uniform float Radius;
uniform float Time;
uniform float Kind;
uniform float Fusion;
uniform float Stretch;
uniform float Opacity;
in vec3 Ray;
in vec3 Plane;
out vec4 fragColor;

float hash(vec3 p){p=fract(p*.1031);p+=dot(p,p.yzx+33.33);return fract((p.x+p.y)*p.z);}
float noise(vec3 p){
    vec3 i=floor(p),f=fract(p);f=f*f*(3.0-2.0*f);
    return mix(mix(mix(hash(i),hash(i+vec3(1,0,0)),f.x),mix(hash(i+vec3(0,1,0)),hash(i+vec3(1,1,0)),f.x),f.y),
               mix(mix(hash(i+vec3(0,0,1)),hash(i+vec3(1,0,1)),f.x),mix(hash(i+vec3(0,1,1)),hash(i+vec3(1,1,1)),f.x),f.y),f.z);
}
float fbm(vec3 p){float v=0.0,a=.55;for(int i=0;i<4;i++){v+=a*noise(p);p=p*2.07+vec3(9.3,2.8,7.1);a*=.48;}return v;}
mat2 turn(float a){return mat2(cos(a),-sin(a),sin(a),cos(a));}
vec3 warp(vec3 v){return v-Axis*dot(v,Axis)*(1.0-1.0/max(Stretch,1.0));}
float segment(vec2 p,vec2 a,vec2 b){vec2 d=b-a;return length(p-a-d*clamp(dot(p-a,d)/max(dot(d,d),.0001),0.0,1.0));}
vec3 background(vec2 uv){return texture(Scene,clamp(uv,vec2(.001),vec2(.999))).rgb;}

void main(){
    vec3 ray=normalize(Ray),oc=warp(-Center),d=warp(ray);
    float a=dot(d,d),b=dot(oc,d),c=dot(oc,oc)-Radius*Radius;
    float discriminant=b*b-a*c;
    float nearest=max(0.01,-b/a);
    float distanceToAxis=length(oc+d*nearest)/Radius;
    if(distanceToAxis>1.70)discard;
    vec2 uv=vec2(dot(Plane,Right),dot(Plane,Up))/Radius;
    vec2 screen=(gl_FragCoord.xy-Viewport)/Resolution;
    float time=Time,angle=atan(uv.y,uv.x),r=length(uv);
    float turbulent=fbm(vec3(uv*3.0,time*.9));
    float halo=pow(max(0.0,1.0-distanceToAxis/1.62),3.0);
    float lens=exp(-pow((distanceToAxis-.97)/.40,2.0))*(1.0-smoothstep(1.3,1.68,distanceToAxis));
    vec2 bend=normalize(uv+vec2(.001))*(.008+.017*turbulent)*lens*(Kind>1.5?2.1:1.0);
    bend+=vec2(sin(angle*7.0+time*3.0),cos(angle*5.0-time*4.0))*.008*lens;
    vec3 scene=background(screen+bend);
    vec3 tint=Kind<.5?vec3(.01,.58,1.0):Kind<1.5?vec3(1.0,.035,.055):vec3(.43,.025,1.0);
    if(Fusion>.5)tint=Kind<.5?vec3(.05,.005,1):vec3(1,.005,.045);
    vec3 color=scene+tint*halo*.15;
    float alpha=max(halo*.48,lens*(Kind>1.5?.72:.45));
    float t=nearest;

    if(discriminant>0.0){
        t=(-b-sqrt(discriminant))/a;
        if(t<=.02)discard;
        vec3 local=warp(ray*t-Center)/Radius;
        vec3 normal=normalize(warp(local));
        float facing=max(.0,dot(normal,-ray));
        vec3 flow=local;
        flow.xy=turn(time*(Kind<.5?.37:-.29))*flow.xy;
        vec3 curl=vec3(fbm(flow*2.8+time*.21),fbm(flow.zxy*2.7-time*.17),fbm(flow.yzx*2.9+4.0));
        float cloud=fbm(flow*4.0+curl*3.4+vec3(0,time*.35,-time*.24));
        float detail=fbm(flow*8.0-curl*1.8+time*.4);
        float veins=pow(1.0-abs(sin(cloud*12.0+detail*5.0)),5.0);
        float rim=pow(1.0-facing,2.5);

        if(Fusion>.5){
            // Only the two ingredients of Purple use the dark marbled reference.
            vec3 dark=Kind<.5?vec3(.002,.0005,.025):vec3(.025,.0003,.003);
            vec3 hot=Kind<.5?vec3(.075,.006,1.0):vec3(1.0,.006,.15);
            float energy=smoothstep(.24,.77,cloud)*(.5+.5*detail);
            color=mix(dark,hot,energy)*(.34+.66*pow(facing,.3));
            color+=hot*veins*.045;
            color=mix(color,dark,rim*.72);
            alpha=.99*smoothstep(.0,.12,facing);
        }else if(Kind<1.5){
            // Standalone Blue: cyan plasma; standalone Red: compact hot shell.
            vec2 coreUV=uv+vec2(sin(time*4.0),cos(time*3.0))*.025;
            float core=Kind<.5?1.0-smoothstep(.38,.67,length(coreUV)+(cloud-.5)*.25):1.0-smoothstep(.22,.50,length(coreUV)+(cloud-.5)*.20);
            vec3 body=Kind<.5?vec3(.0,.75,1):vec3(1,.04,.065);
            color=body*(.48+.55*cloud+veins*.06);
            color+=body*rim*.75;
            color=mix(color,vec3(1.0,.99,1.0),core*.98);
            color+=vec3(1,.7,.8)*pow(veins,2.0)*(Kind<.5?.04:.10);
            alpha=Kind<.5?.96*smoothstep(.0,.35,facing):.96;
        }else{
            // An opaque violet volume, glassy edge and irregular luminous interior.
            vec3 interior=flow*.55+vec3(time*.12,-time*.18,time*.1);
            float inner=fbm(interior*5.0+curl*1.4);
            vec2 coreUV=uv-vec2(.06*sin(time*.6),.05*cos(time*.8));
            float coreDistance=length(coreUV)+(inner-.5)*.65;
            float core=1.0-smoothstep(.11,.39,coreDistance);
            float fire=exp(-coreDistance*4.8)*(.55+inner);
            color=mix(vec3(.035,.001,.13),vec3(.33,.008,.8),cloud*.85);
            color+=vec3(.72,.055,1)*fire;
            color+=vec3(.24,.008,.42)*pow(1.0-abs(sin(inner*13.0+flow.y*5.0+time*2.0)),4.0)*exp(-length(uv)*2.5);
            color=mix(color,vec3(1.3,.95,1.22),core);
            color+=vec3(.14,.035,.55)*rim*2.0;
            color=mix(color,scene*vec3(.35,.12,.65),rim*.18);
            // Fine stars live within the projected sphere, not on external hoops.
            vec2 grid=uv*24.0;vec2 cell=floor(grid);
            vec2 point=vec2(hash(vec3(cell,2)),hash(vec3(cell,5)))*.70+.15;
            float spark=1.0-smoothstep(.015,.065,length(fract(grid)-point));
            spark*=step(.68,hash(vec3(cell,9)))*(.35+.65*pow(.5+.5*sin(time*3.0+hash(vec3(cell,3))*40.0),4.0));
            color+=vec3(1,.65,1)*spark*.8;
            alpha=.97;
        }
    }else if(Fusion<.5&&Kind<.5){
        // Ragged cyan tongues outside the standalone Blue core.
        float teeth=.25*(fbm(vec3(cos(angle)*7.0,sin(angle)*7.0,time*2.3))-.5)+.16*sin(angle*27.0+time*11.0)+.10*sin(angle*13.0-time*8.0);
        float flame=(1.0-smoothstep(.93,1.48+teeth,distanceToAxis))*(.65+turbulent);
        color=scene+vec3(.0,.95,1.10)*flame*1.15;alpha=max(alpha,flame*.88);
    }else if(Fusion<.5&&Kind<1.5){
        vec2 grid=uv*12.0;vec2 cell=floor(grid);
        float spark=1.0-smoothstep(.0,.075,length(fract(grid)-vec2(hash(vec3(cell,3)),hash(vec3(cell,8)))));
        spark*=step(.78,hash(vec3(cell,floor(time*8.0))))*(1.0-smoothstep(1.1,1.65,distanceToAxis));
        color+=vec3(1,.55,.30)*spark;alpha=max(alpha,spark*.9);
    }

    if(Kind>1.5&&Fusion<.5){
        float bolt=0.0;
        for(int branch=0;branch<4;branch++){
            float seed=float(branch)*11.7+floor(time*9.0);
            float direction=hash(vec3(seed,2,4))*6.28318;
            vec2 axis=vec2(cos(direction),sin(direction)),side=vec2(-axis.y,axis.x);
            vec2 previous=axis*.15;
            for(int stepIndex=1;stepIndex<=5;stepIndex++){
                float k=float(stepIndex)/5.0;
                vec2 next=axis*(.15+1.28*k)+side*(hash(vec3(seed,float(stepIndex),6))-.5)*.35;
                float dist=segment(uv,previous,next);
                bolt+=exp(-dist*120.0)*.9+exp(-dist*32.0)*.22;
                previous=next;
            }
        }
        float flash=.6+.4*sin(time*31.0);
        color+=vec3(1.0,.62,1.0)*bolt*flash;
        alpha=max(alpha,clamp(bolt*flash,.0,.98));
    }
    if(alpha*Opacity<.008)discard;
    vec4 projected=OrbProjection*OrbMat*vec4(ray*t,1.0);
    float surfaceDepth=clamp(projected.z/projected.w*.5+.5,0.0,1.0);
    if(surfaceDepth>texture(WorldDepth,screen).r+.000025)discard;
    gl_FragDepth=surfaceDepth;
    fragColor=vec4(min(color,vec3(1.35)),clamp(alpha*Opacity,0.0,1.0));
}
