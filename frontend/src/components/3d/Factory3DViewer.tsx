import React, { useEffect, useRef, useState } from 'react';
import * as THREE from 'three';
import { OrbitControls } from 'three/examples/jsm/controls/OrbitControls.js';
import {
  Eye, Cpu, Activity,
  Zap, Flame, Gauge, Sparkles, Maximize2, Play, Pause, Compass
} from 'lucide-react';

interface Factory3DViewerProps {
  selectedFactory?: any;
  machines?: any[];
  robots?: any[];
  simData?: any;
  simulationRunning?: boolean;
  onSelectEquipment?: (equipment: any) => void;
}

interface Equipment3DData {
  id: string | number;
  name: string;
  type: string;
  category: 'machine' | 'robot' | 'conveyor';
  position: [number, number, number];
  status: 'OPERATIONAL' | 'WARNING' | 'FAULT' | 'IDLE';
  health: number; // 0 - 100
  power: number; // kW
  speed: number; // RPM or mm/s
  temperature: number; // °C
  vibration: number; // mm/s
  meshGroup: THREE.Group;
  spindleMesh?: THREE.Mesh;
  robotJoints?: THREE.Group[];
  pistonMesh?: THREE.Mesh;
  workpieces?: THREE.Mesh[];
  details: any;
}

export const Factory3DViewer: React.FC<Factory3DViewerProps> = ({
  selectedFactory,
  machines = [],
  robots = [],
  simData,
  simulationRunning = false,
  onSelectEquipment,
}) => {
  const containerRef = useRef<HTMLDivElement>(null);
  const [selectedItem, setSelectedItem] = useState<Equipment3DData | null>(null);
  const [hoveredItem, setHoveredItem] = useState<Equipment3DData | null>(null);
  const [isAnimRunning, setIsAnimRunning] = useState<boolean>(true);
  const [viewPreset, setViewPreset] = useState<'iso' | 'top' | 'front'>('iso');
  const [lightingTheme, setLightingTheme] = useState<'cyber' | 'industrial' | 'studio'>('cyber');

  // Internal Three.js references
  const sceneRef = useRef<THREE.Scene | null>(null);
  const cameraRef = useRef<THREE.PerspectiveCamera | null>(null);
  const rendererRef = useRef<THREE.WebGLRenderer | null>(null);
  const controlsRef = useRef<OrbitControls | null>(null);
  const equipmentMapRef = useRef<Map<string | number, Equipment3DData>>(new Map());
  const reqAnimIdRef = useRef<number | null>(null);
  const raycasterRef = useRef(new THREE.Raycaster());
  const mouseRef = useRef(new THREE.Vector2());

  // Setup Three.js Scene
  useEffect(() => {
    if (!containerRef.current) return;
    const container = containerRef.current;
    const width = container.clientWidth;
    const height = container.clientHeight;

    // 1. Scene
    const scene = new THREE.Scene();
    scene.background = new THREE.Color(0x090d16);
    scene.fog = new THREE.FogExp2(0x090d16, 0.018);
    sceneRef.current = scene;

    // 2. Camera
    const camera = new THREE.PerspectiveCamera(45, width / height, 0.1, 1000);
    camera.position.set(22, 16, 22);
    cameraRef.current = camera;

    // 3. Renderer
    const renderer = new THREE.WebGLRenderer({ antialias: true, alpha: false, powerPreference: 'high-performance' });
    renderer.setSize(width, height);
    renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));
    renderer.shadowMap.enabled = true;
    renderer.shadowMap.type = THREE.PCFSoftShadowMap;
    renderer.toneMapping = THREE.ACESFilmicToneMapping;
    renderer.toneMappingExposure = 1.1;
    container.appendChild(renderer.domElement);
    rendererRef.current = renderer;

    // 4. Controls
    const controls = new OrbitControls(camera, renderer.domElement);
    controls.enableDamping = true;
    controls.dampingFactor = 0.05;
    controls.maxPolarAngle = Math.PI / 2 - 0.02; // Prevent going under floor
    controls.minDistance = 5;
    controls.maxDistance = 80;
    controls.target.set(0, 2, 0);
    controlsRef.current = controls;

    // 5. Lighting
    setupLighting(scene, lightingTheme);

    // 6. Environment & Floor
    setupFloor(scene);

    // 7. Render Loop
    let clock = new THREE.Clock();
    const animate = () => {
      reqAnimIdRef.current = requestAnimationFrame(animate);
      const delta = clock.getDelta();
      const elapsedTime = clock.getElapsedTime();

      controls.update();

      // Animate Equipment Kinematics & Belts
      if (isAnimRunning || simulationRunning) {
        equipmentMapRef.current.forEach((item) => {
          // Machine Spindle rotation
          if (item.spindleMesh) {
            item.spindleMesh.rotation.y += delta * (item.status === 'OPERATIONAL' ? 12 : 2);
          }

          // Hydraulic Piston reciprocating motion
          if (item.pistonMesh) {
            const freq = item.status === 'OPERATIONAL' ? 3 : 1;
            item.pistonMesh.position.y = 2.2 + Math.sin(elapsedTime * freq) * 0.4;
          }

          // Robotic Arm Articulated Joint motion
          if (item.robotJoints && item.robotJoints.length >= 3) {
            const speedMult = item.status === 'OPERATIONAL' ? 1.5 : 0.4;
            // Base rotation
            item.robotJoints[0].rotation.y = Math.sin(elapsedTime * 0.8 * speedMult) * 0.8;
            // Shoulder flex
            item.robotJoints[1].rotation.z = Math.sin(elapsedTime * 1.2 * speedMult) * 0.3 - 0.2;
            // Elbow flex
            item.robotJoints[2].rotation.z = Math.cos(elapsedTime * 1.5 * speedMult) * 0.4 + 0.3;
          }

          // Conveyor belt workpiece sliding
          if (item.workpieces && item.workpieces.length > 0) {
            item.workpieces.forEach((wp, idx) => {
              wp.position.x += delta * 2.5;
              if (wp.position.x > 6) wp.position.x = -6 + (idx * 2);
            });
          }

          // Status light subtle pulse
          const statusLight = item.meshGroup.getObjectByName('statusLightPoint');
          if (statusLight && statusLight instanceof THREE.PointLight) {
            const pulse = item.status === 'FAULT' ? Math.sin(elapsedTime * 10) * 1.5 + 2 : 1;
            statusLight.intensity = (item.status === 'OPERATIONAL' ? 2 : 3) * pulse;
          }
        });
      }

      renderer.render(scene, camera);
    };

    animate();

    // 8. Resize Handler
    const handleResize = () => {
      if (!containerRef.current || !rendererRef.current || !cameraRef.current) return;
      const w = containerRef.current.clientWidth;
      const h = containerRef.current.clientHeight;
      cameraRef.current.aspect = w / h;
      cameraRef.current.updateProjectionMatrix();
      rendererRef.current.setSize(w, h);
    };

    window.addEventListener('resize', handleResize);

    return () => {
      window.removeEventListener('resize', handleResize);
      if (reqAnimIdRef.current) cancelAnimationFrame(reqAnimIdRef.current);
      if (rendererRef.current && rendererRef.current.domElement) {
        container.removeChild(rendererRef.current.domElement);
        rendererRef.current.dispose();
      }
    };
  }, []);

  // Update Lighting Theme
  useEffect(() => {
    if (sceneRef.current) {
      setupLighting(sceneRef.current, lightingTheme);
    }
  }, [lightingTheme]);

  // Rebuild 3D Equipment Objects when machines/robots prop update or on initial render
  useEffect(() => {
    if (!sceneRef.current) return;
    const scene = sceneRef.current;

    // Clear old equipment meshes
    equipmentMapRef.current.forEach((item) => {
      scene.remove(item.meshGroup);
    });
    equipmentMapRef.current.clear();

    // Build items list (from props or default synthetic factory layout)
    const itemsToRender = buildEquipmentData(machines, robots, selectedFactory, simData);

    itemsToRender.forEach((item) => {
      const meshGroup = createEquipmentMesh(item);
      meshGroup.position.set(...item.position);
      scene.add(meshGroup);

      item.meshGroup = meshGroup;
      item.spindleMesh = meshGroup.getObjectByName('spindle') as THREE.Mesh;
      item.pistonMesh = meshGroup.getObjectByName('piston') as THREE.Mesh;

      // Extract robot joints
      const j0 = meshGroup.getObjectByName('joint0') as THREE.Group;
      const j1 = meshGroup.getObjectByName('joint1') as THREE.Group;
      const j2 = meshGroup.getObjectByName('joint2') as THREE.Group;
      if (j0 && j1 && j2) item.robotJoints = [j0, j1, j2];

      // Extract conveyor workpieces
      const wp1 = meshGroup.getObjectByName('wp1') as THREE.Mesh;
      const wp2 = meshGroup.getObjectByName('wp2') as THREE.Mesh;
      const wp3 = meshGroup.getObjectByName('wp3') as THREE.Mesh;
      if (wp1 && wp2) item.workpieces = [wp1, wp2, wp3].filter(Boolean) as THREE.Mesh[];

      equipmentMapRef.current.set(item.id, item);
    });

  }, [machines, robots, selectedFactory, simData]);

  // Mouse Raycasting & Hover / Selection Logic
  const handlePointerMove = (e: React.MouseEvent<HTMLDivElement>) => {
    if (!containerRef.current || !cameraRef.current || !sceneRef.current) return;
    const rect = containerRef.current.getBoundingClientRect();
    mouseRef.current.x = ((e.clientX - rect.left) / rect.width) * 2 - 1;
    mouseRef.current.y = -((e.clientY - rect.top) / rect.height) * 2 + 1;

    raycasterRef.current.setFromCamera(mouseRef.current, cameraRef.current);

    const interactiveMeshes: THREE.Object3D[] = [];
    equipmentMapRef.current.forEach((item) => {
      interactiveMeshes.push(item.meshGroup);
    });

    const intersects = raycasterRef.current.intersectObjects(interactiveMeshes, true);
    if (intersects.length > 0) {
      let topObj: THREE.Object3D | null = intersects[0].object;
      while (topObj && !topObj.userData.equipmentId && topObj.parent) {
        topObj = topObj.parent;
      }
      if (topObj && topObj.userData.equipmentId) {
        const item = equipmentMapRef.current.get(topObj.userData.equipmentId);
        if (item && item.id !== hoveredItem?.id) {
          setHoveredItem(item);
          containerRef.current.style.cursor = 'pointer';
        }
        return;
      }
    }

    if (hoveredItem) {
      setHoveredItem(null);
      containerRef.current.style.cursor = 'default';
    }
  };

  const handleClick = () => {
    if (hoveredItem) {
      setSelectedItem(hoveredItem);
      if (onSelectEquipment) onSelectEquipment(hoveredItem.details);

      // Smooth camera focus
      if (cameraRef.current && controlsRef.current) {
        const targetPos = new THREE.Vector3(...hoveredItem.position);
        controlsRef.current.target.copy(targetPos);
        controlsRef.current.update();
      }
    }
  };

  // View Preset Camera Switching
  const applyViewPreset = (preset: 'iso' | 'top' | 'front') => {
    setViewPreset(preset);
    if (!cameraRef.current || !controlsRef.current) return;
    const controls = controlsRef.current;
    const camera = cameraRef.current;

    controls.target.set(0, 1.5, 0);

    if (preset === 'iso') {
      camera.position.set(22, 16, 22);
    } else if (preset === 'top') {
      camera.position.set(0, 32, 0.001);
    } else if (preset === 'front') {
      camera.position.set(0, 6, 28);
    }
    controls.update();
  };

  return (
    <div className="relative w-full h-full min-h-[550px] bg-slate-950 rounded-2xl overflow-hidden border border-slate-800 shadow-2xl flex flex-col">
      {/* 3D Canvas Container */}
      <div
        ref={containerRef}
        onPointerMove={handlePointerMove}
        onClick={handleClick}
        className="w-full flex-1 relative bg-gradient-to-b from-slate-950 via-slate-900 to-slate-950"
      >
        {/* Top Control Bar Overlay */}
        <div className="absolute top-4 left-4 right-4 flex items-center justify-between pointer-events-none z-10">
          {/* Status Badge */}
          <div className="flex items-center gap-3 bg-slate-900/80 backdrop-blur-md px-4 py-2 rounded-xl border border-slate-800 pointer-events-auto shadow-lg">
            <div className="relative flex items-center justify-center">
              <span className={`h-3 w-3 rounded-full ${simulationRunning ? 'bg-emerald-400 animate-ping' : 'bg-cyan-400'}`} />
              <span className={`absolute h-2.5 w-2.5 rounded-full ${simulationRunning ? 'bg-emerald-500' : 'bg-cyan-500'}`} />
            </div>
            <div>
              <p className="text-[10px] uppercase font-bold tracking-wider text-slate-400">3D Digital Twin Engine</p>
              <p className="text-xs font-semibold text-slate-200">
                {selectedFactory?.name || 'Main Manufacturing Hall'} &nbsp;•&nbsp; {equipmentMapRef.current.size} Units Active
              </p>
            </div>
          </div>

          {/* Camera View Presets Toolbar */}
          <div className="flex items-center gap-1.5 bg-slate-900/80 backdrop-blur-md p-1.5 rounded-xl border border-slate-800 pointer-events-auto shadow-lg">
            <button
              onClick={() => applyViewPreset('iso')}
              className={`px-3 py-1.5 text-xs font-semibold rounded-lg transition flex items-center gap-1.5 ${
                viewPreset === 'iso' ? 'bg-cyan-500 text-slate-950 shadow-md font-bold' : 'text-slate-400 hover:text-slate-200 hover:bg-slate-800/60'
              }`}
            >
              <Compass className="h-3.5 w-3.5" /> 3D Iso
            </button>
            <button
              onClick={() => applyViewPreset('top')}
              className={`px-3 py-1.5 text-xs font-semibold rounded-lg transition flex items-center gap-1.5 ${
                viewPreset === 'top' ? 'bg-cyan-500 text-slate-950 shadow-md font-bold' : 'text-slate-400 hover:text-slate-200 hover:bg-slate-800/60'
              }`}
            >
              <Maximize2 className="h-3.5 w-3.5" /> Top Floor
            </button>
            <button
              onClick={() => applyViewPreset('front')}
              className={`px-3 py-1.5 text-xs font-semibold rounded-lg transition flex items-center gap-1.5 ${
                viewPreset === 'front' ? 'bg-cyan-500 text-slate-950 shadow-md font-bold' : 'text-slate-400 hover:text-slate-200 hover:bg-slate-800/60'
              }`}
            >
              <Eye className="h-3.5 w-3.5" /> Front Elevation
            </button>

            <div className="h-4 w-[1px] bg-slate-800 mx-1" />

            <button
              onClick={() => setIsAnimRunning(!isAnimRunning)}
              title={isAnimRunning ? 'Pause Animation' : 'Play Kinematics'}
              className="p-1.5 text-slate-400 hover:text-cyan-400 hover:bg-slate-800/60 rounded-lg transition"
            >
              {isAnimRunning ? <Pause className="h-4 w-4" /> : <Play className="h-4 w-4 text-emerald-400" />}
            </button>

            {/* Lighting theme switcher */}
            <select
              value={lightingTheme}
              onChange={(e) => setLightingTheme(e.target.value as any)}
              className="bg-slate-950 text-slate-300 text-xs px-2 py-1.5 rounded-lg border border-slate-800 focus:outline-none cursor-pointer"
            >
              <option value="cyber">Cyberpunk Lighting</option>
              <option value="industrial">Industrial Daylight</option>
              <option value="studio">Clean Studio</option>
            </select>
          </div>
        </div>

        {/* Hover Equipment Preview Floating Card */}
        {hoveredItem && !selectedItem && (
          <div className="absolute bottom-6 left-6 pointer-events-none z-10 bg-slate-900/90 backdrop-blur-xl p-4 rounded-xl border border-cyan-500/40 shadow-2xl animate-in fade-in slide-in-from-bottom-2 duration-150 max-w-xs">
            <div className="flex items-center gap-2 mb-1">
              <span className={`h-2.5 w-2.5 rounded-full ${getStatusColorBg(hoveredItem.status)}`} />
              <h4 className="text-sm font-bold text-slate-100">{hoveredItem.name}</h4>
            </div>
            <p className="text-[11px] text-cyan-400 font-semibold mb-2 uppercase">{hoveredItem.type}</p>
            <div className="grid grid-cols-2 gap-2 text-[10px] text-slate-400 border-t border-slate-800 pt-2">
              <div>
                <span className="block text-slate-500">Status</span>
                <span className="font-semibold text-slate-200">{hoveredItem.status}</span>
              </div>
              <div>
                <span className="block text-slate-500">Health Index</span>
                <span className="font-semibold text-emerald-400">{hoveredItem.health}%</span>
              </div>
            </div>
            <p className="text-[9px] text-slate-500 mt-2 text-center">Click unit to open 3D Telemetry HUD</p>
          </div>
        )}

        {/* Selected Equipment 3D Telemetry HUD Modal */}
        {selectedItem && (
          <div className="absolute top-16 right-6 bottom-6 w-96 bg-slate-900/95 backdrop-blur-2xl p-5 rounded-2xl border border-cyan-500/30 shadow-2xl z-20 flex flex-col overflow-y-auto animate-in slide-in-from-right duration-200">
            <div className="flex items-center justify-between pb-3 border-b border-slate-800 mb-4">
              <div className="flex items-center gap-2.5">
                <div className="p-2 bg-cyan-500/10 border border-cyan-500/30 rounded-xl">
                  <Cpu className="h-5 w-5 text-cyan-400" />
                </div>
                <div>
                  <h3 className="text-base font-extrabold text-slate-100">{selectedItem.name}</h3>
                  <span className="text-[10px] font-bold uppercase tracking-wider text-cyan-400">{selectedItem.type}</span>
                </div>
              </div>
              <button
                onClick={() => setSelectedItem(null)}
                className="text-slate-400 hover:text-slate-100 p-1 rounded-lg hover:bg-slate-800 transition"
              >
                ✕
              </button>
            </div>

            {/* Status & Health Gauge Header */}
            <div className="bg-slate-950/80 p-4 rounded-xl border border-slate-800 mb-4 space-y-3">
              <div className="flex items-center justify-between">
                <span className="text-xs text-slate-400 font-medium">Operating Condition</span>
                <span className={`px-2.5 py-0.5 rounded-full text-[10px] font-extrabold tracking-wide text-white ${getStatusBadgeColor(selectedItem.status)}`}>
                  {selectedItem.status}
                </span>
              </div>

              {/* Health Score Bar */}
              <div>
                <div className="flex justify-between text-xs mb-1">
                  <span className="text-slate-400">Health Score</span>
                  <span className="font-bold text-slate-200">{selectedItem.health}%</span>
                </div>
                <div className="w-full h-2 bg-slate-800 rounded-full overflow-hidden">
                  <div
                    className={`h-full rounded-full transition-all duration-500 ${
                      selectedItem.health > 80 ? 'bg-emerald-500' : selectedItem.health > 50 ? 'bg-amber-500' : 'bg-red-500'
                    }`}
                    style={{ width: `${selectedItem.health}%` }}
                  />
                </div>
              </div>
            </div>

            {/* Simulated Live Telemetry Sensors Grid */}
            <h4 className="text-xs font-bold uppercase tracking-wider text-slate-400 mb-2 flex items-center gap-1.5">
              <Activity className="h-3.5 w-3.5 text-cyan-400" /> Live Sensor Telemetry
            </h4>
            <div className="grid grid-cols-2 gap-2.5 mb-5">
              <div className="p-3 bg-slate-950/60 rounded-xl border border-slate-800 flex items-center gap-3">
                <Zap className="h-5 w-5 text-amber-400 flex-shrink-0" />
                <div>
                  <p className="text-[10px] text-slate-500 uppercase font-semibold">Power Load</p>
                  <p className="text-sm font-bold text-slate-200">{selectedItem.power} <span className="text-[10px] font-normal text-slate-400">kW</span></p>
                </div>
              </div>

              <div className="p-3 bg-slate-950/60 rounded-xl border border-slate-800 flex items-center gap-3">
                <Gauge className="h-5 w-5 text-cyan-400 flex-shrink-0" />
                <div>
                  <p className="text-[10px] text-slate-500 uppercase font-semibold">Spindle Speed</p>
                  <p className="text-sm font-bold text-slate-200">{selectedItem.speed} <span className="text-[10px] font-normal text-slate-400">RPM</span></p>
                </div>
              </div>

              <div className="p-3 bg-slate-950/60 rounded-xl border border-slate-800 flex items-center gap-3">
                <Flame className="h-5 w-5 text-rose-400 flex-shrink-0" />
                <div>
                  <p className="text-[10px] text-slate-500 uppercase font-semibold">Operating Temp</p>
                  <p className="text-sm font-bold text-slate-200">{selectedItem.temperature} <span className="text-[10px] font-normal text-slate-400">°C</span></p>
                </div>
              </div>

              <div className="p-3 bg-slate-950/60 rounded-xl border border-slate-800 flex items-center gap-3">
                <Sparkles className="h-5 w-5 text-violet-400 flex-shrink-0" />
                <div>
                  <p className="text-[10px] text-slate-500 uppercase font-semibold">Vibration Level</p>
                  <p className="text-sm font-bold text-slate-200">{selectedItem.vibration} <span className="text-[10px] font-normal text-slate-400">mm/s</span></p>
                </div>
              </div>
            </div>

            {/* Equipment Spec Details */}
            <h4 className="text-xs font-bold uppercase tracking-wider text-slate-400 mb-2">Unit Metadata</h4>
            <div className="bg-slate-950/60 p-3 rounded-xl border border-slate-800 text-xs space-y-1.5 text-slate-300 mb-5">
              <div className="flex justify-between"><span className="text-slate-500">Equipment ID:</span> <span className="font-mono">{selectedItem.id}</span></div>
              <div className="flex justify-between"><span className="text-slate-500">Category:</span> <span>{selectedItem.category}</span></div>
              <div className="flex justify-between"><span className="text-slate-500">Position Vector:</span> <span className="font-mono text-[10px]">[{selectedItem.position.join(', ')}]</span></div>
            </div>

            {/* Close Button */}
            <button
              onClick={() => setSelectedItem(null)}
              className="mt-auto w-full py-2.5 bg-gradient-to-r from-cyan-600 to-violet-600 hover:from-cyan-500 hover:to-violet-500 text-white font-bold text-xs rounded-xl shadow-lg transition"
            >
              Back to Overview
            </button>
          </div>
        )}
      </div>
    </div>
  );
};

// --- HELPER: Setup Lighting ---
function setupLighting(scene: THREE.Scene, theme: 'cyber' | 'industrial' | 'studio') {
  // Remove existing lights
  const existingLights = scene.children.filter((c) => c instanceof THREE.Light);
  existingLights.forEach((l) => scene.remove(l));

  if (theme === 'cyber') {
    scene.background = new THREE.Color(0x070a12);
    if (scene.fog) scene.fog.color = new THREE.Color(0x070a12);

    const ambient = new THREE.AmbientLight(0x1e293b, 1.5);
    scene.add(ambient);

    const dirLight = new THREE.DirectionalLight(0x38bdf8, 2.0);
    dirLight.position.set(15, 25, 15);
    dirLight.castShadow = true;
    dirLight.shadow.mapSize.width = 2048;
    dirLight.shadow.mapSize.height = 2048;
    scene.add(dirLight);

    const rimLight = new THREE.DirectionalLight(0x8b5cf6, 1.8);
    rimLight.position.set(-15, 15, -15);
    scene.add(rimLight);
  } else if (theme === 'industrial') {
    scene.background = new THREE.Color(0x0f172a);
    if (scene.fog) scene.fog.color = new THREE.Color(0x0f172a);

    const ambient = new THREE.AmbientLight(0xffffff, 1.2);
    scene.add(ambient);

    const dirLight = new THREE.DirectionalLight(0xfef08a, 2.2);
    dirLight.position.set(20, 30, 10);
    dirLight.castShadow = true;
    scene.add(dirLight);
  } else {
    scene.background = new THREE.Color(0x111827);
    if (scene.fog) scene.fog.color = new THREE.Color(0x111827);

    const ambient = new THREE.AmbientLight(0xffffff, 2.0);
    scene.add(ambient);
  }
}

// --- HELPER: Setup Floor Grid ---
function setupFloor(scene: THREE.Scene) {
  // Industrial metallic floor mesh
  const floorGeo = new THREE.PlaneGeometry(60, 60);
  const floorMat = new THREE.MeshStandardMaterial({
    color: 0x0f172a,
    roughness: 0.35,
    metalness: 0.7,
  });
  const floor = new THREE.Mesh(floorGeo, floorMat);
  floor.rotation.x = -Math.PI / 2;
  floor.receiveShadow = true;
  scene.add(floor);

  // Futuristic Grid Helper
  const grid = new THREE.GridHelper(60, 30, 0x00f0ff, 0x1e293b);
  grid.position.y = 0.01;
  scene.add(grid);

  // Yellow/black perimeter safety border
  const borderGeo = new THREE.BoxGeometry(40, 0.05, 40);
  const borderMat = new THREE.MeshBasicMaterial({ color: 0xeab308, wireframe: true });
  const border = new THREE.Mesh(borderGeo, borderMat);
  border.position.y = 0.02;
  scene.add(border);
}

// --- HELPER: Build equipment array from props or generate realistic default factory ---
function buildEquipmentData(machines: any[], robots: any[], _factory: any, _simData: any): Equipment3DData[] {
  const list: Equipment3DData[] = [];

  // Parse machines from prop or fallback
  if (machines && machines.length > 0) {
    machines.forEach((m, idx) => {
      const col = idx % 3;
      const row = Math.floor(idx / 3);
      list.push({
        id: `machine-${m.id || idx}`,
        name: m.name || `CNC Machine #${idx + 1}`,
        type: m.type || 'CNC Cutter',
        category: 'machine',
        position: [-10 + col * 10, 0, -8 + row * 10],
        status: m.operationalHours > 500 ? 'WARNING' : 'OPERATIONAL',
        health: Math.max(40, 100 - (idx * 8)),
        power: m.powerConsumption || 15.5,
        speed: m.operatingSpeed || 1200,
        temperature: 42 + idx * 4,
        vibration: 0.8 + idx * 0.3,
        meshGroup: new THREE.Group(),
        details: m,
      });
    });
  } else {
    // Default Synthetic Factory Layout
    list.push(
      {
        id: 'cnc-1',
        name: '5-Axis CNC Machining Center',
        type: 'CNC Milling',
        category: 'machine',
        position: [-10, 0, -6],
        status: 'OPERATIONAL',
        health: 96,
        power: 18.2,
        speed: 3200,
        temperature: 41,
        vibration: 0.4,
        meshGroup: new THREE.Group(),
        details: { name: '5-Axis CNC Machining Center', manufacturer: 'Siemens' },
      },
      {
        id: 'cnc-2',
        name: 'High Precision CNC Lathe',
        type: 'CNC Lathe',
        category: 'machine',
        position: [0, 0, -6],
        status: 'OPERATIONAL',
        health: 88,
        power: 14.0,
        speed: 2800,
        temperature: 48,
        vibration: 0.9,
        meshGroup: new THREE.Group(),
        details: { name: 'High Precision CNC Lathe', manufacturer: 'Haas Automation' },
      },
      {
        id: 'press-1',
        name: '200-Ton Hydraulic Stamping Press',
        type: 'Hydraulic Press',
        category: 'machine',
        position: [10, 0, -6],
        status: 'WARNING',
        health: 64,
        power: 45.0,
        speed: 120,
        temperature: 62,
        vibration: 2.4,
        meshGroup: new THREE.Group(),
        details: { name: '200-Ton Hydraulic Stamping Press', manufacturer: 'Komatsu' },
      },
      {
        id: 'robot-1',
        name: 'ABB IRB 1200 Robot Arm',
        type: '6-DOF Articulated Robot',
        category: 'robot',
        position: [-5, 0, 4],
        status: 'OPERATIONAL',
        health: 99,
        power: 4.8,
        speed: 1800,
        temperature: 36,
        vibration: 0.2,
        meshGroup: new THREE.Group(),
        details: { name: 'ABB IRB 1200 Robot Arm', payload: '7kg' },
      },
      {
        id: 'robot-2',
        name: 'KUKA KR 10 Pick & Place',
        type: 'Industrial Robot Arm',
        category: 'robot',
        position: [5, 0, 4],
        status: 'OPERATIONAL',
        health: 92,
        power: 5.2,
        speed: 2100,
        temperature: 39,
        vibration: 0.3,
        meshGroup: new THREE.Group(),
        details: { name: 'KUKA KR 10 Pick & Place', payload: '10kg' },
      },
      {
        id: 'conveyor-1',
        name: 'Main Assembly Conveyor Belt',
        type: 'Automated Conveyor',
        category: 'conveyor',
        position: [0, 0, 12],
        status: 'OPERATIONAL',
        health: 95,
        power: 8.5,
        speed: 500,
        temperature: 34,
        vibration: 0.5,
        meshGroup: new THREE.Group(),
        details: { name: 'Main Assembly Conveyor Belt' },
      }
    );
  }

  // Parse robots if provided
  if (robots && robots.length > 0) {
    robots.forEach((r, idx) => {
      list.push({
        id: `robot-prop-${r.id || idx}`,
        name: r.name || `Robot Arm #${idx + 1}`,
        type: r.model || 'Articulated Robot',
        category: 'robot',
        position: [-8 + idx * 8, 0, 4],
        status: 'OPERATIONAL',
        health: 95,
        power: 6.0,
        speed: 1500,
        temperature: 38,
        vibration: 0.3,
        meshGroup: new THREE.Group(),
        details: r,
      });
    });
  }

  return list;
}

// --- HELPER: Create Detailed Procedural 3D Mesh for Equipment ---
function createEquipmentMesh(item: Equipment3DData): THREE.Group {
  const group = new THREE.Group();
  group.userData = { equipmentId: item.id };

  const statusColorHex = getStatusHexColor(item.status);

  if (item.type.includes('CNC') || item.category === 'machine' && !item.name.includes('Press')) {
    // === CNC Machine Model ===
    // Main Body Enclosure
    const bodyGeo = new THREE.BoxGeometry(3.6, 2.4, 2.8);
    const bodyMat = new THREE.MeshStandardMaterial({ color: 0x334155, metalness: 0.8, roughness: 0.3 });
    const body = new THREE.Mesh(bodyGeo, bodyMat);
    body.position.y = 1.2;
    body.castShadow = true;
    body.receiveShadow = true;
    group.add(body);

    // Glass Front Window
    const glassGeo = new THREE.BoxGeometry(2.4, 1.4, 0.1);
    const glassMat = new THREE.MeshPhysicalMaterial({
      color: 0x38bdf8,
      transparent: true,
      opacity: 0.4,
      roughness: 0.1,
      metalness: 0.1,
      transmission: 0.9,
    });
    const glass = new THREE.Mesh(glassGeo, glassMat);
    glass.position.set(0, 1.4, 1.41);
    group.add(glass);

    // Spindle Tool Head (rotates in animation loop)
    const spindleGeo = new THREE.CylinderGeometry(0.25, 0.25, 0.8, 16);
    const spindleMat = new THREE.MeshStandardMaterial({ color: 0x94a3b8, metalness: 0.9, roughness: 0.1 });
    const spindle = new THREE.Mesh(spindleGeo, spindleMat);
    spindle.name = 'spindle';
    spindle.position.set(0, 1.2, 0);
    group.add(spindle);

    // Status Pillar Light Tower
    addStatusTowerLight(group, statusColorHex, [1.5, 2.5, 1.1]);

  } else if (item.name.includes('Press') || item.type.includes('Press')) {
    // === Hydraulic Press Model ===
    // Base & Bed
    const bedGeo = new THREE.BoxGeometry(4, 0.6, 3);
    const bedMat = new THREE.MeshStandardMaterial({ color: 0x1e293b, metalness: 0.9, roughness: 0.4 });
    const bed = new THREE.Mesh(bedGeo, bedMat);
    bed.position.y = 0.3;
    bed.castShadow = true;
    group.add(bed);

    // 4 Corner Steel Posts
    const postGeo = new THREE.CylinderGeometry(0.18, 0.18, 4.2, 16);
    const postMat = new THREE.MeshStandardMaterial({ color: 0x64748b, metalness: 0.9, roughness: 0.2 });

    const pos = [[-1.7, -1.2], [1.7, -1.2], [-1.7, 1.2], [1.7, 1.2]];
    pos.forEach(([px, pz]) => {
      const post = new THREE.Mesh(postGeo, postMat);
      post.position.set(px, 2.1, pz);
      post.castShadow = true;
      group.add(post);
    });

    // Top Crown Block
    const crownGeo = new THREE.BoxGeometry(4.2, 0.8, 3.2);
    const crownMat = new THREE.MeshStandardMaterial({ color: 0x0f172a, metalness: 0.8, roughness: 0.3 });
    const crown = new THREE.Mesh(crownGeo, crownMat);
    crown.position.y = 4.2;
    crown.castShadow = true;
    group.add(crown);

    // Hydraulic Reciprocating Piston
    const pistonGeo = new THREE.CylinderGeometry(0.6, 0.6, 1.6, 32);
    const pistonMat = new THREE.MeshStandardMaterial({ color: 0xe2e8f0, metalness: 0.95, roughness: 0.05 });
    const piston = new THREE.Mesh(pistonGeo, pistonMat);
    piston.name = 'piston';
    piston.position.set(0, 2.2, 0);
    piston.castShadow = true;
    group.add(piston);

    addStatusTowerLight(group, statusColorHex, [1.9, 4.7, 1.3]);

  } else if (item.category === 'robot' || item.type.includes('Robot')) {
    // === 6-DOF Articulated Robot Arm Model ===
    // Pedestal Base
    const baseGeo = new THREE.CylinderGeometry(0.8, 1.0, 0.8, 24);
    const baseMat = new THREE.MeshStandardMaterial({ color: 0x0284c7, metalness: 0.7, roughness: 0.3 });
    const base = new THREE.Mesh(baseGeo, baseMat);
    base.position.y = 0.4;
    base.castShadow = true;
    group.add(base);

    // Joint 0: Swivel Shoulder
    const j0 = new THREE.Group();
    j0.name = 'joint0';
    j0.position.set(0, 0.8, 0);
    group.add(j0);

    const shoulderGeo = new THREE.BoxGeometry(0.7, 0.7, 0.7);
    const armMat = new THREE.MeshStandardMaterial({ color: 0x0ea5e9, metalness: 0.8, roughness: 0.2 });
    const shoulder = new THREE.Mesh(shoulderGeo, armMat);
    j0.add(shoulder);

    // Joint 1: Lower Arm Link
    const j1 = new THREE.Group();
    j1.name = 'joint1';
    j1.position.set(0, 0.35, 0);
    j0.add(j1);

    const link1Geo = new THREE.CylinderGeometry(0.2, 0.25, 1.6, 16);
    const link1 = new THREE.Mesh(link1Geo, armMat);
    link1.position.y = 0.8;
    link1.castShadow = true;
    j1.add(link1);

    // Joint 2: Upper Arm Link & Wrist Gripper
    const j2 = new THREE.Group();
    j2.name = 'joint2';
    j2.position.set(0, 1.6, 0);
    j1.add(j2);

    const link2Geo = new THREE.CylinderGeometry(0.16, 0.2, 1.4, 16);
    const link2 = new THREE.Mesh(link2Geo, armMat);
    link2.position.y = 0.7;
    link2.rotation.z = Math.PI / 4;
    link2.castShadow = true;
    j2.add(link2);

    // Gripper end-effector
    const gripGeo = new THREE.BoxGeometry(0.3, 0.2, 0.4);
    const gripMat = new THREE.MeshStandardMaterial({ color: 0x334155, metalness: 0.9, roughness: 0.2 });
    const grip = new THREE.Mesh(gripGeo, gripMat);
    grip.position.set(0.5, 1.2, 0);
    j2.add(grip);

    addStatusTowerLight(group, statusColorHex, [0.6, 1.2, 0.6]);

  } else {
    // === Conveyor Belt System Model ===
    const frameGeo = new THREE.BoxGeometry(14, 0.4, 1.8);
    const frameMat = new THREE.MeshStandardMaterial({ color: 0x475569, metalness: 0.8, roughness: 0.4 });
    const frame = new THREE.Mesh(frameGeo, frameMat);
    frame.position.y = 0.8;
    frame.castShadow = true;
    group.add(frame);

    // Support Legs
    const legGeo = new THREE.CylinderGeometry(0.08, 0.08, 0.8, 12);
    const legMat = new THREE.MeshStandardMaterial({ color: 0x1e293b, metalness: 0.9 });

    [-6, -2, 2, 6].forEach((lx) => {
      [-0.7, 0.7].forEach((lz) => {
        const leg = new THREE.Mesh(legGeo, legMat);
        leg.position.set(lx, 0.4, lz);
        group.add(leg);
      });
    });

    // Moving Workpieces on Belt
    const wpGeo = new THREE.BoxGeometry(0.6, 0.5, 0.6);
    const wpMat = new THREE.MeshStandardMaterial({ color: 0xd97706, metalness: 0.3, roughness: 0.4 });

    for (let i = 1; i <= 3; i++) {
      const wp = new THREE.Mesh(wpGeo, wpMat);
      wp.name = `wp${i}`;
      wp.position.set(-6 + i * 4, 1.25, 0);
      wp.castShadow = true;
      group.add(wp);
    }

    addStatusTowerLight(group, statusColorHex, [6.5, 1.2, 0.8]);
  }

  return group;
}

// --- HELPER: Add glowing status tower beacon light ---
function addStatusTowerLight(group: THREE.Group, colorHex: number, pos: [number, number, number]) {
  const towerGeo = new THREE.CylinderGeometry(0.12, 0.12, 0.6, 16);
  const towerMat = new THREE.MeshStandardMaterial({
    color: colorHex,
    emissive: colorHex,
    emissiveIntensity: 2.5,
    roughness: 0.2,
  });
  const tower = new THREE.Mesh(towerGeo, towerMat);
  tower.position.set(...pos);
  group.add(tower);

  const pointLight = new THREE.PointLight(colorHex, 2.5, 6);
  pointLight.name = 'statusLightPoint';
  pointLight.position.set(...pos);
  group.add(pointLight);
}

// --- COLOR UTILS ---
function getStatusHexColor(status: string): number {
  switch (status) {
    case 'OPERATIONAL': return 0x10b981; // Emerald Green
    case 'WARNING':     return 0xf59e0b; // Amber Yellow
    case 'FAULT':       return 0xef4444; // Crimson Red
    default:            return 0x06b6d4; // Cyan Standby
  }
}

function getStatusColorBg(status: string): string {
  switch (status) {
    case 'OPERATIONAL': return 'bg-emerald-500 shadow-emerald-500/50 shadow-md';
    case 'WARNING':     return 'bg-amber-500 shadow-amber-500/50 shadow-md';
    case 'FAULT':       return 'bg-red-500 shadow-red-500/50 shadow-md animate-pulse';
    default:            return 'bg-cyan-500 shadow-cyan-500/50 shadow-md';
  }
}

function getStatusBadgeColor(status: string): string {
  switch (status) {
    case 'OPERATIONAL': return 'bg-emerald-600/90';
    case 'WARNING':     return 'bg-amber-600/90';
    case 'FAULT':       return 'bg-red-600/90';
    default:            return 'bg-cyan-600/90';
  }
}
