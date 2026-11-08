"""Original reference-shaped OBJ meshes and dedicated materials. No vanilla block geometry/textures."""
from pathlib import Path
import math,json,random
from PIL import Image
ROOT=Path(__file__).resolve().parents[1]/'src/main/resources/assets/unlimitedvoid'
def add(a,b):return tuple(x+y for x,y in zip(a,b))
def sub(a,b):return tuple(x-y for x,y in zip(a,b))
def mul(a,s):return tuple(x*s for x in a)
def dot(a,b):return sum(x*y for x,y in zip(a,b))
def cross(a,b):return (a[1]*b[2]-a[2]*b[1],a[2]*b[0]-a[0]*b[2],a[0]*b[1]-a[1]*b[0])
def unit(a):return mul(a,1/max(1e-10,math.sqrt(dot(a,a))))
class Mesh:
    def __init__(self):self.lines=['# Original Jujutsu Domains mesh','mtllib cursed_tools.mtl'];self.n=0;self.faces=0
    def face(self,pts,mat):
        normal=unit(cross(sub(pts[1],pts[0]),sub(pts[2],pts[0])))
        if dot(normal,normal)<.1:return
        self.lines.append('usemtl '+mat)
        for i,p in enumerate(pts):
            self.lines.append('v '+' '.join(f'{x/16:.7f}' for x in p))
            self.lines.append('vt '+('0 0' if i==0 else '1 0' if i==1 else '1 1' if i==2 else '0 1'))
            self.lines.append('vn '+' '.join(f'{x:.7f}' for x in normal))
        ids=[self.n+i+1 for i in range(len(pts))]
        self.lines.append('f '+' '.join(f'{i}/{i}/{i}' for i in ids));self.n+=len(pts);self.faces+=1
    def tube(self,a,b,r,mat,sides=20,r2=None):
        n=unit(sub(b,a));u=unit(cross(n,(0,0,1) if abs(n[2])<.9 else (1,0,0)));v=cross(n,u)
        def ring(p,radius):return [add(p,add(mul(u,radius*math.cos(i*2*math.pi/sides)),mul(v,radius*math.sin(i*2*math.pi/sides)))) for i in range(sides)]
        x=ring(a,r);y=ring(b,r if r2 is None else r2)
        for i in range(sides):j=(i+1)%sides;self.face([x[i],x[j],y[j],y[i]],mat)
        for i in range(1,sides-1):self.face([x[0],x[i+1],x[i]],mat);self.face([y[0],y[i],y[i+1]],mat)
    def torus(self,center,u,v,rx,ry,thickness,mat,segments=20):
        u=unit(u);v=unit(v);normal=unit(cross(u,v));rings=[]
        for i in range(segments):
            a=i*2*math.pi/segments;out=unit(add(mul(u,math.cos(a)),mul(v,math.sin(a))))
            p=add(center,add(mul(u,rx*math.cos(a)),mul(v,ry*math.sin(a))))
            rings.append([add(p,add(mul(out,thickness*math.cos(j*math.pi/3)),mul(normal,thickness*math.sin(j*math.pi/3)))) for j in range(6)])
        for i in range(segments):
            for j in range(6):self.face([rings[i][j],rings[(i+1)%segments][j],rings[(i+1)%segments][(j+1)%6],rings[i][(j+1)%6]],mat)
    def blade(self,poly,z=8):
        if sum(poly[i][0]*poly[(i+1)%len(poly)][1]-poly[(i+1)%len(poly)][0]*poly[i][1] for i in range(len(poly)))<0:poly=list(reversed(poly))
        cx=sum(p[0] for p in poly)/len(poly);cy=sum(p[1] for p in poly)/len(poly)
        inner=[(cx+(x-cx)*.92,cy+(y-cy)*.97) for x,y in poly]
        indices=list(range(len(inner)));triangles=[]
        def area(a,b,c):return (b[0]-a[0])*(c[1]-a[1])-(b[1]-a[1])*(c[0]-a[0])
        while len(indices)>3:
            for j,b in enumerate(indices):
                a=indices[j-1];c=indices[(j+1)%len(indices)]
                if area(inner[a],inner[b],inner[c])<=1e-8:continue
                if any(k not in (a,b,c) and min(area(inner[a],inner[b],inner[k]),area(inner[b],inner[c],inner[k]),area(inner[c],inner[a],inner[k]))>=-1e-8 for k in indices):continue
                triangles.append((a,b,c));indices.pop(j);break
            else:raise ValueError('Blade outline cannot be triangulated')
        triangles.append(tuple(indices))
        for sign in (-1,1):
            for t in triangles:
                pts=[(*inner[k],z+sign*.25) for k in t];self.face(pts if sign>0 else pts[::-1],'steel')
            for i in range(len(poly)):
                j=(i+1)%len(poly);pts=[(*poly[i],z+sign*.035),(*poly[j],z+sign*.035),(*inner[j],z+sign*.25),(*inner[i],z+sign*.25)]
                self.face(pts if sign>0 else pts[::-1],'edge')
        for i in range(len(poly)):
            j=(i+1)%len(poly);self.face([(*poly[i],z+.035),(*poly[i],z-.035),(*poly[j],z-.035),(*poly[j],z+.035)],'shadow')
    def grip(self,x,start,end,z=8,radius=.48):
        self.tube((x,start,z),(x,end,z),radius,'black',24)
        turns=(end-start)/.55;steps=int(turns*20)
        for i in range(steps):
            a=i/20*2*math.pi;b=(i+1)/20*2*math.pi;y=start+i/steps*(end-start)
            r=radius+.025
            self.face([(x+r*math.cos(a),y,z+r*math.sin(a)),(x+r*math.cos(b),y+(end-start)/steps,z+r*math.sin(b)),(x+r*math.cos(b),y+.20+(end-start)/steps,z+r*math.sin(b)),(x+r*math.cos(a),y+.20,z+r*math.sin(a))],'wrap')
        for y in (start,end-.2):self.tube((x,y,z),(x,y+.2,z),radius+.08,'shadow',24)
    def save(self,name,gui_scale=.65):
        folder=ROOT/'models/item';folder.mkdir(parents=True,exist_ok=True)
        (folder/(name+'.obj')).write_text('\n'.join(self.lines)+'\n',encoding='utf-8')
        display={'gui':{'rotation':[8,-28,-35],'translation':[0,-2,0],'scale':[gui_scale]*3},
            'ground':{'rotation':[0,0,90],'translation':[0,2,0],'scale':[.35]*3},
            'fixed':{'rotation':[0,0,-25],'translation':[0,-1,0],'scale':[.6]*3},
            'thirdperson_righthand':{'rotation':[0,90,55],'translation':[0,3,1],'scale':[.75]*3},
            'thirdperson_lefthand':{'rotation':[0,-90,-55],'translation':[0,3,1],'scale':[.75]*3},
            'firstperson_righthand':{'rotation':[0,-90,25],'translation':[1,3,1],'scale':[.75]*3},
            'firstperson_lefthand':{'rotation':[0,90,-25],'translation':[1,3,1],'scale':[.75]*3}}
        model={'loader':'forge:obj','model':f'unlimitedvoid:models/item/{name}.obj','automatic_culling':False,'shade_quads':True,'emissive_ambient':False,'flip_v':True,'gui_light':'side','textures':{'particle':'unlimitedvoid:item/cursed_steel'},'display':display}
        (folder/(name+'.json')).write_text(json.dumps(model,indent=2)+'\n',encoding='utf-8')
        print(f'{name}: {self.n} vertices, {self.faces} faces')

def generate():
    materials={'steel':(164,174,183),'edge':(230,235,239),'shadow':(77,84,92),'black':(19,21,25),'wrap':(40,43,48),'red':(155,10,22),'fur':(224,218,199),'furshade':(160,153,132)}
    texture=ROOT/'textures/item';texture.mkdir(parents=True,exist_ok=True);mtl=[]
    for name,rgb in materials.items():
        im=Image.new('RGBA',(64,64));rng=random.Random(name)
        for y in range(64):
            for x in range(64):
                grain=rng.uniform(-2,2);shine=math.sin(x/64*math.pi)*(.1 if name in ('steel','edge','red') else .025)
                if name in ('fur','furshade'):grain+=3*math.sin(x*.9+y*.18)
                im.putpixel((x,y),tuple(max(0,min(255,int(c*(.93+shine)+grain))) for c in rgb)+(255,))
        im.save(texture/f'cursed_{name}.png')
        mtl += ['newmtl '+name,'Kd 1 1 1',f'map_Kd unlimitedvoid:item/cursed_{name}','']
    (ROOT/'models/item/cursed_tools.mtl').write_text('\n'.join(mtl),encoding='utf-8')
    m=Mesh();m.grip(8,.7,8,radius=.70)
    m.torus((8,.25,8),(1,0,0),(0,1,0),.34,.65,.13,'shadow')
    for y in (8,8.25):
        # Oval, ridged steel handguard; not a gold rectangular crossbar.
        m.torus((8,y,8),(1,0,0),(0,0,1),1.65,.75,.15,'steel',32)
    m.tube((8,8,8),(8,8.3,8),1.05,'shadow',24)
    m.blade([(6.6,8.3),(5.8,10),(5.5,25),(7,23.5),(8,21),(8,14),(9,12.5),(10.1,13),(10,16.1),(10.8,19.4),(12.5,17.6),(13,13.1),(12.6,10.8),(10.2,9),(9.3,8.3)])
    m.face([(7.3,9.8,8.27),(7.48,9.8,8.27),(7.48,12.1,8.27),(7.3,12.1,8.27)],'shadow')
    m.face([(6.6,10.65,8.27),(8.5,10.65,8.27),(8.5,10.85,8.27),(6.6,10.85,8.27)],'shadow')
    m.save('inverted_spear_of_heaven',.63)
    m=Mesh();m.grip(8,-2.8,8,radius=.72);m.blade([(6.9,8.6),(7.0,31),(7.2,35),(9.2,33.5),(9.5,10.2),(9.1,8.6)])
    m.torus((8,8.2,8),(1,0,0),(0,0,1),1.5,.8,.3,'fur')
    rng=random.Random(181)
    for i in range(42):
        a=i*2.39996;rx=1.0+rng.random()*1.15;rz=.65+rng.random()*.65;y=8.2+rng.uniform(-.35,.3)
        center=(8+math.cos(a)*rx,y,8+math.sin(a)*rz);w=.28+rng.random()*.22
        tip=add(center,(math.cos(a)*.35,rng.choice([-1,1])*(.5+rng.random()*.8),math.sin(a)*.35))
        pts=[add(center,(-w,0,-w)),add(center,(w,.12,-w)),add(center,(w,0,w)),add(center,(-w,.1,w))]
        for j in range(4):m.face([pts[j],pts[(j+1)%4],tip],'fur' if i%4 else 'furshade')
    m.save('split_soul_katana',.51)
    m=Mesh();rods=[((2,0,8),(2.6,17,8)),((5.6,18.5,8),(8.7,1.8,8)),((11,.8,8),(14.3,17.5,8))]
    for a,b in rods:
        direction=unit(sub(b,a));m.tube(a,b,.35,'red',28)
        for end,sign in ((a,1),(b,-1)):
            for j in range(13):
                p=add(end,mul(direction,sign*j*.14));m.tube(p,add(p,mul(direction,sign*.10)),.39 if j%3 else .43,'black',20)
            m.tube(add(end,mul(direction,sign*1.95)),add(end,mul(direction,sign*2.15)),.40,'shadow',24)
            m.tube(add(end,mul(direction,-sign*.3)),end,.19,'shadow',16)
    for start,end in ((rods[0][1],rods[1][0]),(rods[1][1],rods[2][0])):
        axis=unit(sub(end,start));crossaxis=unit(cross(axis,(0,0,1)))
        for i in range(9):
            t=(i+.5)/9;center=add(add(mul(start,1-t),mul(end,t)),(0,-.45*math.sin(t*math.pi),0))
            m.torus(center,axis,crossaxis if i%2 else (0,0,1),.30,.22,.065,'shadow',16)
    m.save('playful_cloud',.75)
    m=Mesh()
    for segment in range(3):
        a=(8,-8+segment*16,8);b=(8,8+segment*16,8)
        m.tube(a,b,.35,'red',28)
        for y in (a[1],b[1]-.45):m.tube((8,y,8),(8,y+.45,8),.42,'shadow',24)
    for end in (-8,38):
        for i in range(15):m.tube((8,end+i*.12,8),(8,end+i*.12+.08,8),.40,'black',20)
    m.save('playful_cloud_staff',.39)
    m=Mesh();m.tube((8,0,8),(8,17,8),.35,'red',28)
    for i in range(18):m.tube((8,i*.14,8),(8,i*.14+.1,8),.41,'black',20)
    # Chipped, sloping sharpened tip exposes the core instead of adding a magical blade.
    m.tube((8,16.8,8),(7.78,20,8),.35,'edge',20,r2=.015)
    m.tube((8,-.25,8),(8,0,8),.23,'shadow',16)
    m.save('playful_cloud_blade',.8)
    path=ROOT/'models/item/playful_cloud.json';data=json.loads(path.read_text())
    data['overrides']=[{'predicate':{'unlimitedvoid:cloud_form':1},'model':'unlimitedvoid:item/playful_cloud_staff'},
                       {'predicate':{'unlimitedvoid:cloud_form':2},'model':'unlimitedvoid:item/playful_cloud_blade'}]
    path.write_text(json.dumps(data,indent=2)+'\n',encoding='utf-8')
if __name__=='__main__':generate()
