package org.etieskrill.game.horde3d;

//FIXME
//public class Application extends App {
//
//    private static final int FRAME_RATE = 60;
//
//    private static final Loaders.ModelLoader MODELS = Loaders.ModelLoader.get();
//
//    private static final Logger logger = LoggerFactory.getLogger(Application.class);
//
//    private Model floorModel;
//    private Transform floorTransform;
//    private Model[] brickCubes;
//    private Transform[] brickCubeTransforms;
//    private StaticShader shader;
//    private Camera camera;
//
//    private DirectionalLight sunLight;
//    private Model sunModel;
//    private Transform sunTransform;
//    private PointLight light1;
//    private Model lightModel1;
//    private Transform lightTransform1;
//    private PointLight light2;
//    private Model lightModel2;
//    private Transform lightTransform2;
//    private LightSourceShader lightShader;
//    private boolean light = true;
//    private static final Vector3fc lightOn = new Vector3f(1);
//    private static final Vector3fc lightOff = new Vector3f(0);
//
//    private Matrix4f sunLightCombined;
//    private DirectionalShadowMap directionalShadowMap;
//    private Shaders.DepthShader depthShader;
//    private Matrix4fc[] pointLightCombined1;
//    private Matrix4fc[] pointLightCombined2;
//    private PointShadowMapArray pointShadowMaps;
//    private DepthCubeMapArrayShader depthCubeMapArrayShader;
//    private final float pointShadowNearPlane = .1f;
//    private final float pointShadowFarPlane = 40;
//
//    private Label fpsLabel;
//
//    private final Window window = getWindow();
//    private final GLRenderer renderer = getRenderer();
//    private final LoopPacer pacer = getPacer();
//
//    public Application() {
//        super(new Window(
//                Window.WindowSize.INSTANCE.getDEFAULT(),
//                Window.WindowMode.BORDERLESS,
//                "Horde 3D",
//                FRAME_RATE,
//                null,
//                new Cursor(Cursor.CursorShape.ARROW),
//                false,
//                true,
//                4,
//                false,
//                false
//        ));
//    }
//
//    @Override
//    public void init() {
//        GLUtils.addDebugLogging();
//
//        floorModel = ModelFactory.box(new Vector3f(100, .1f, 100));
//        Material floorMaterial = floorModel.getNodes().get(2).getMeshes().getFirst().getMaterial();
//        floorMaterial.setProperty(Material.Property.SHININESS, 256f);
//        floorMaterial.getTextures().clear();
//        floorMaterial.getTextures().add(Textures.ofFile("TilesSlateSquare001_COL_2K_METALNESS.png", DIFFUSE));
//        floorMaterial.getTextures().add(Textures.ofFile("TilesSlateSquare001_ROUGHNESS_2K_METALNESS.png", SPECULAR));
//        floorMaterial.getTextures().add(
//                new Texture2D.FileBuilder("TilesSlateSquare001_NRM_2K_METALNESS.png", NORMAL)
//                        .setFormat(AbstractTexture.Format.RGB) //TODO MMMMMMHHHHH select correct format automatically
//                        .build()
//        );
//
//        floorTransform.setPosition(new Vector3f(0, -1, 0));
//
//        Model sphere = MODELS.load("sphere", () -> Model.ofFile("Sphere.obj"));
//
//        sunLight = new DirectionalLight(new Vector3f(-1), new Vector3f(.1f), new Vector3f(.5f), new Vector3f(2));
//
//        sunModel = new Model(sphere);
//        sunTransform.setPosition(new Vector3f(50));
//        sunTransform.setScale(new Vector3f(.35f));
//
//        light1 = new PointLight(new Vector3f(10, 0, 10),
//                new Vector3f(.1f), new Vector3f(2), new Vector3f(2),
//                1, .14f, .07f);
//        lightModel1 = new Model(sphere);
//        lightTransform1.setPosition(light1.getPosition());
//        lightTransform1.setScale(new Vector3f(.01f));
//
//        light2 = new PointLight(new Vector3f(-10, 0, -10),
//                new Vector3f(.1f), new Vector3f(2), new Vector3f(2),
//                1, .14f, .07f);
//        lightModel2 = new Model(sphere);
//        lightTransform2.setPosition(light2.getPosition());
//        lightTransform2.setScale(new Vector3f(.01f));
//
//        MODELS.load("brick-cube", () -> Model.ofFile("brick-cube.obj"));
//        brickCubes = new Model[10];
//        brickCubeTransforms = new Transform[10];
//        Random random = new Random(69420);
//        for (int i = 0; i < brickCubes.length; i++) {
//            brickCubes[i] = MODELS.get("brick-cube");
//            brickCubeTransforms[i] = new Transform(
//                    new Vector3f(random.nextFloat() * 30 - 15, random.nextFloat() * 3, random.nextFloat() * 30 - 15),
//                    new Quaternionf().rotationAxis((float) (random.nextFloat() * 2 * Math.PI - Math.PI),
//                            new Vector3f(random.nextFloat(), random.nextFloat(), random.nextFloat())
//                            .mul(2).sub(1, 1, 1)
//                            .normalize()),
//                    new Vector3f(3f)
//            );
//        }
//
//        lightShader = new LightSourceShader();
//        shader = new StaticShader();
//
//        camera = new PerspectiveCamera(window.getSize());
//        window.getCursorInputs().add(new CursorCameraController(camera));
//        KeyCameraController cameraController = (KeyCameraController) new KeyCameraController(camera).setSpeed(3);
//        window.getKeyInputs().add(cameraController);
//        window.getCursor().disable();
//
//        window.addKeyInputs(Input.of(
//                Input.bind(Keys.Q).to(() -> {
//                    light = !light;
//                    logger.info("Turning sunlight {}", light ? "on" : "off");
//
//                    sunLight.setAmbient(light ? lightOn : lightOff);
//                    sunLight.setDiffuse(light ? lightOn : lightOff);
//                    sunLight.setSpecular(light ? lightOn : lightOff);
//                }),
//                Input.bind(Keys.CTRL).to(() -> {
//                    if (cameraController.getSpeed() == 3) cameraController.setSpeed(20);
//                    else cameraController.setSpeed(3);
//                })
//        ));
//
//        directionalShadowMap = DirectionalShadowMap.generate(new Vector2i(1024));
//        pointShadowMaps = PointShadowMapArray.generate(new Vector2i(1024), 2);
//
//        sunLightCombined = new Matrix4f()
//                .ortho(-30, 30, -30, 30, .1f, 40)
//                .mul(new Matrix4f().lookAt(new Vector3f(10, 20, 10), new Vector3f(-10, 0, -10), new Vector3f(0, 1, 0)));
//
//        pointLightCombined1 = pointShadowMaps.getCombinedMatrices(pointShadowNearPlane, pointShadowFarPlane, light1);
//        pointLightCombined2 = pointShadowMaps.getCombinedMatrices(pointShadowNearPlane, pointShadowFarPlane, light2);
//
//        depthShader = new Shaders.DepthShader();
//        depthCubeMapArrayShader = new DepthCubeMapArrayShader();
//
//        glEnable(GL_FRAMEBUFFER_SRGB);
//
/// /        GLUtils.removeDebugLogging();
//
//        OrthographicCamera uiCamera = new OrthographicCamera(window.getSize().getVec());
//        uiCamera.setPosition(new Vector3f(window.getSize().getVec(), 0).div(2));
//        fpsLabel = new Label("", Fonts.getDefault(36));
//        fpsLabel.setAlignment(Node.Alignment.TOP_LEFT);
//        fpsLabel.setMargin(new Vector4f(10));
//        window.setScene(new Scene(new Batch(getScreenBuffer(), renderer, window.getCurrentSize()), new Container(fpsLabel), uiCamera));
//    }
//
//    @Override
//    protected void loop(double delta) {
//        sunTransform.setPosition(new Vector3f(50).add(camera.getPosition()));
//
//        directionalShadowMap.bind();
//        glClear(GL_DEPTH_BUFFER_BIT);
/// /        glCullFace(GL_FRONT); //Helps with peter panning, but back faces intersecting with other shadowed objects peter pan instead
//        renderScene(depthShader, sunLightCombined);
////        glCullFace(GL_BACK);
//        directionalShadowMap.unbind();
//
//        /*
//        So the depth buffer is initialised to zero at first (as is basically any buffer), and clearing it sets all its
//        values to 1.0f, which is equal to the far plane representing the maximum depth any fragment can have, as
//        anything behind it is clipped.
//        */
//        pointShadowMaps.bind();
//        glClear(GL_DEPTH_BUFFER_BIT);
//        renderScene(0, light1, depthCubeMapArrayShader, pointLightCombined1);
//        renderScene(1, light2, depthCubeMapArrayShader, pointLightCombined2);
//        pointShadowMaps.unbind();
//
//        renderer.prepare();
//        glViewport(0, 0, 1920, 1080); //TODO make render manager (??? entity?) do this
//        renderScene(shader, camera.getCombined());
//        renderLights();
//
//        if (pacer.getTotalFramesElapsed() % 60 == 0) {
//            logger.info("Fps: {}, gpu time: {}ms, gpu delay: {}ms",
//                    "%4.1f".formatted(pacer.getAverageFPS()),
//                    "%5.2f".formatted(renderer.getAveragedGpuTime() / 1_000_000.0),
//                    "%5.2f".formatted(renderer.getGpuDelay() / 1_000_000.0));
//        }
//        fpsLabel.setText("%5.3f".formatted(pacer.getAverageFPS()));
//    }
//
//    private void renderScene(Shaders.DepthShader shader, Matrix4fc combined) {
//        renderer.render(floorTransform, floorModel, shader, combined);
//        for (int i = 0; i < brickCubes.length; i++) {
//            renderer.render(brickCubeTransforms[i], brickCubes[i], shader, combined);
//        }
//    }
//
//    private void renderScene(int index, PointLight light, DepthCubeMapArrayShader shader, Matrix4fc[] combined) {
//        DepthCubeMapArrayShaderKt.setLight(shader, light);
//        DepthCubeMapArrayShaderKt.setFarPlane(shader, pointShadowFarPlane);
//        DepthCubeMapArrayShaderKt.setShadowCombined(shader, combined);
//        DepthCubeMapArrayShaderKt.setIndex(shader, index);
//        renderer.render(floorTransform, floorModel, shader, new Matrix4f());
//        for (int i = 0; i < brickCubes.length; i++) {
//            renderer.render(brickCubeTransforms[i], brickCubes[i], shader, new Matrix4f());
//        }
//    }
//
//    private void renderScene(StaticShader shader, Matrix4fc combined) {
//        renderer.bindNextFreeTexture(shader, "shadowMap", directionalShadowMap.getTexture());
//        shader.setUniform("lightCombined", sunLightCombined, false);
//        renderer.bindNextFreeTexture(shader, "pointShadowMaps0", pointShadowMaps.getTexture());
//        StaticShaderKt.setGlobalLights(shader, new Object[]{sunLight});
//        StaticShaderKt.setLights(shader, new Object[]{light1, light2});
//        StaticShaderKt.setViewPosition(shader, camera.getPosition());
//        StaticShaderKt.setTextureScale(shader, new Vector2f(15));
//        StaticShaderKt.setPointShadowFarPlane(shader, pointShadowFarPlane);
//        renderer.render(floorTransform, floorModel, shader, combined);
//
//        StaticShaderKt.setTextureScale(shader, new Vector2f(1));
//        for (int i = 0; i < brickCubes.length; i++) {
//            renderer.render(brickCubeTransforms[i], brickCubes[i], shader, combined);
//        }
//    }
//
//    private void renderLights() {
//        LightSourceShaderKt.setLight(lightShader, sunLight);
//        renderer.render(sunTransform, sunModel, lightShader, camera.getCombined());
//        LightSourceShaderKt.setLight(lightShader, light1);
//        renderer.render(lightTransform1, lightModel1, lightShader, camera.getCombined());
//        LightSourceShaderKt.setLight(lightShader, light2);
//        renderer.render(lightTransform2, lightModel2, lightShader, camera.getCombined());
//    }
//
//    public static void main(String[] args) {
//        new Application();
//    }
//}
