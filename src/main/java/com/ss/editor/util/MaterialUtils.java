package com.ss.editor.util;

import static com.ss.editor.util.EditorUtil.*;
import static com.ss.rlib.common.util.ObjectUtils.notNull;
import static com.ss.rlib.common.util.array.ArrayFactory.toArray;
import static java.nio.file.StandardOpenOption.*;
import com.jme3.asset.AssetKey;
import com.jme3.asset.MaterialKey;
import com.jme3.asset.TextureKey;
import com.jme3.material.MatParam;
import com.jme3.material.Material;
import com.jme3.material.MaterialDef;
import com.jme3.scene.Spatial;
import com.jme3.shader.VarType;
import com.jme3.texture.Texture;
import com.ss.editor.FileExtensions;
import com.ss.editor.annotation.FromAnyThread;
import com.ss.editor.annotation.JmeThread;
import com.ss.rlib.common.util.FileUtils;
import com.ss.rlib.common.util.StringUtils;
import jme3tools.converters.ImageToAwt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.imageio.ImageIO;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

/**
 * The class with utility methods for working with {@link Material}.
 *
 * @author JavaSaBr
 */
public class MaterialUtils {

    @NotNull
    private static final String[][] TEXTURE_TYPE_PARAM_NAMES = {
            toArray(""),
            toArray("DiffuseMap", "BaseColorMap"),
            toArray("NormalMap"),
            toArray("EmissiveMap", "GlowMap"),
            toArray("MetallicMap"),
            toArray("RoughnessMap"),
            toArray("SpecularMap"),
    };

    private static final int TEXTURE_DIFFUSE = 1;
    private static final int TEXTURE_NORMAL = 2;
    private static final int TEXTURE_EMISSIVE = 3;
    private static final int TEXTURE_METALLIC = 4;
    private static final int TEXTURE_ROUGHNESS = 5;
    private static final int TEXTURE_SPECULAR = 6;

    /**
     * Get a possible texture type of dropped texture.
     *
     * @param textureName the texture name.
     * @return the texture type or 0.
     */
    @FromAnyThread
    public static int getPossibleTextureType(@NotNull String textureName) {

        if (textureName.contains("NORMAL") || textureName.contains("Normal") || textureName.contains("normal")) {
            return TEXTURE_NORMAL;
        } else if (textureName.contains("_NRM") || textureName.contains("_nrm")) {
            return TEXTURE_NORMAL;
        } else if (textureName.contains("_NM") || textureName.contains("_nm")) {
            return TEXTURE_NORMAL;
        } else if (textureName.contains("_N.") || textureName.contains("_n.")) {
            return TEXTURE_NORMAL;
        } else if (textureName.contains("ALBEDO") || textureName.contains("albedo")) {
            return TEXTURE_DIFFUSE;
        } else if (textureName.contains("_CLR") || textureName.contains("_clr")) {
            return TEXTURE_DIFFUSE;
        } else if (textureName.contains("DIFFUSE") || textureName.contains("diffuse")) {
            return TEXTURE_DIFFUSE;
        } else if (textureName.contains("_DIFF") || textureName.contains("_diff")) {
            return TEXTURE_DIFFUSE;
        } else if (textureName.contains("_D.") || textureName.contains("_d.")) {
            return TEXTURE_DIFFUSE;
        }  else if (textureName.contains("_C.") || textureName.contains("_c.")) {
            return TEXTURE_DIFFUSE;
        } else if (textureName.contains("EMISSION") || textureName.contains("emission")) {
            return TEXTURE_EMISSIVE;
        } else if (textureName.contains("GLOW") || textureName.contains("glow")) {
            return TEXTURE_EMISSIVE;
        } else if (textureName.contains("METALLIC") || textureName.contains("metallic")) {
            return TEXTURE_METALLIC;
        } else if (textureName.contains("ROUGHNESS") || textureName.contains("roughness")) {
            return TEXTURE_ROUGHNESS;
        } else if (textureName.contains("SPECULAR") || textureName.contains("specular")) {
            return TEXTURE_SPECULAR;
        } else if (textureName.contains("_SPC") || textureName.contains("_spc")) {
            return TEXTURE_SPECULAR;
        } else if (textureName.contains("_S.") || textureName.contains("_s.")) {
            return TEXTURE_SPECULAR;
        }

        return 0;
    }

    /**
     * Get possible param names for the texture type.
     *
     * @param textureType the texture type.
     * @return the array of possible param names.
     */
    @FromAnyThread
    public static @NotNull String[] getPossibleParamNames(int textureType) {
        return TEXTURE_TYPE_PARAM_NAMES[textureType];
    }

    /**
     * Update a material if need.
     *
     * @param file     the changed file.
     * @param material the current material.
     * @return the updated material or null.
     */
    @JmeThread
    public static @Nullable Material updateMaterialIdNeed(@NotNull Path file, @NotNull Material material) {

        var assetManager = EditorUtil.getAssetManager();

        boolean needToReload = false;
        String textureKey = null;

        if (MaterialUtils.isShaderFile(file)) {

            if (!MaterialUtils.containsShader(material, file)) {
                return null;
            }

            needToReload = true;

            // if the shader was changed we need to reload material definition
            var materialDef = material.getMaterialDef();
            var assetName = materialDef.getAssetName();
            assetManager.deleteFromCache(new AssetKey<>(assetName));

        } else if (MaterialUtils.isTextureFile(file)) {
            textureKey = MaterialUtils.containsTexture(material, file);
            if (textureKey == null) {
                return null;
            }
        }

        var assetName = material.getAssetName();

        // try to refresh texture directly
        if (textureKey != null) {
            refreshTextures(material, textureKey);
            return null;
        } else if (!needToReload || StringUtils.isEmpty(assetName)) {
            return null;
        }

        var materialKey = new MaterialKey(assetName);

        assetManager.deleteFromCache(materialKey);

        var newMaterial = new Material(assetManager, material.getMaterialDef().getAssetName());

        migrateTo(newMaterial, material);

        return newMaterial;
    }

    /**
     * Check a material on containing a shader.
     *
     * @param material the material for checking.
     * @param file     the file of the shader.
     * @return true if the material contains the shader.
     */
    @FromAnyThread
    private static boolean containsShader(@NotNull Material material, @NotNull Path file) {

        var materialDef = material.getMaterialDef();
        var assetFile = notNull(getAssetFile(file), "Can't get an asset file.");
        var assetPath = toAssetPath(assetFile);

        return containsShader(materialDef, assetPath);
    }

    /**
     * Check a material on containing a texture.
     *
     * @param material the material for checking.
     * @param file     the file of the texture.
     * @return changed texture key or null.
     */
    @FromAnyThread
    private static @Nullable String containsTexture(@NotNull Material material, @NotNull Path file) {

        var assetFile = notNull(getAssetFile(file), "Can't get an asset file.");
        var assetPath = toAssetPath(assetFile);

        return containsTexture(material, assetPath) ? assetPath : null;
    }

    /**
     * Check a material definition on containing a shader.
     *
     * @param materialDef the material definition.
     * @param assetPath   the path of the shader.
     * @return true if the material definition contains the shader.
     */
    @FromAnyThread
    private static boolean containsShader(@NotNull MaterialDef materialDef, @NotNull String assetPath) {

        var defaultTechniques = materialDef.getTechniqueDefs("Default");

        for (var technique : defaultTechniques) {
            var shaderProgramNames = technique.getShaderProgramNames();
            if (shaderProgramNames.containsValue(assetPath)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Check a material on containing a texture.
     *
     * @param material  the material.
     * @param assetPath the path of the texture.
     * @return true if the material definition contains the texture.
     */
    @FromAnyThread
    private static boolean containsTexture(@NotNull Material material, @NotNull String assetPath) {

        var materialParams = material.getParams();

        for (var materialParam : materialParams) {

            if (materialParam.getVarType() != VarType.Texture2D) {
                continue;
            }

            var value = (Texture) materialParam.getValue();
            var textureKey = value == null ? null : (TextureKey) value.getKey();
            if (textureKey != null && StringUtils.equals(textureKey.getName(), assetPath)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Is shader file boolean.
     *
     * @param path the file path.
     * @return true if the file is shader.
     */
    @FromAnyThread
    public static boolean isShaderFile(@NotNull Path path) {
        var extension = FileUtils.getExtension(path);
        return FileExtensions.GLSL_FRAGMENT.equals(extension) ||
                FileExtensions.GLSL_VERTEX.equals(extension);
    }

    /**
     * Is texture file boolean.
     *
     * @param path the file path.
     * @return true if the file is texture.
     */
    @FromAnyThread
    public static boolean isTextureFile(@NotNull Path path) {
        var extension = FileUtils.getExtension(path);
        return FileExtensions.IMAGE_DDS.equals(extension) || FileExtensions.IMAGE_HDR.equals(extension) ||
                FileExtensions.IMAGE_HDR.equals(extension) || FileExtensions.IMAGE_JPEG.equals(extension) ||
                FileExtensions.IMAGE_JPG.equals(extension) || FileExtensions.IMAGE_PNG.equals(extension) ||
                FileExtensions.IMAGE_TGA.equals(extension) || FileExtensions.IMAGE_TIFF.equals(extension);

    }

    /**
     * Refresh textures in a material.
     *
     * @param material   the material.
     * @param textureKey the texture key.
     */
    @JmeThread
    private static void refreshTextures(@NotNull Material material, @NotNull String textureKey) {

        var assetManager = EditorUtil.getAssetManager();

        material.getParams().forEach(matParam -> {

            var varType = matParam.getVarType();
            var value = matParam.getValue();

            if (varType != VarType.Texture2D || value == null) {
                return;
            }

            var texture = (Texture) value;
            var key = (TextureKey) texture.getKey();

            if (key != null && StringUtils.equals(key.getName(), textureKey)) {
                var newTexture = assetManager.loadAsset(key);
                matParam.setValue(newTexture);
            }
        });

    }

    /**
     * Update the first material to the second material.
     *
     * @param toUpdate the material for updating.
     * @param material the target material.
     */
    @JmeThread
    private static void updateTo(@NotNull Material toUpdate, @NotNull Material material) {

        var oldParams = new ArrayList<MatParam>(toUpdate.getParams());
        oldParams.forEach(matParam -> {
            var param = material.getParam(matParam.getName());
            if (param == null || param.getValue() == null) {
                toUpdate.clearParam(matParam.getName());
            }
        });

        var actualParams = material.getParams();
        actualParams.forEach(matParam -> {
            var varType = matParam.getVarType();
            var value = matParam.getValue();
            toUpdate.setParam(matParam.getName(), varType, value);
        });

        var additionalRenderState = toUpdate.getAdditionalRenderState();
        additionalRenderState.set(material.getAdditionalRenderState());
    }

    /**
     * Migrate the material to second material.
     *
     * @param target the target migrating.
     * @param source the source material.
     */
    @JmeThread
    public static void migrateTo(@NotNull Material target, @NotNull Material source) {

        var materialDef = target.getMaterialDef();
        var actualParams = source.getParams();

        actualParams.forEach(matParam -> {

            var param = materialDef.getMaterialParam(matParam.getName());

            if (param == null || param.getVarType() != matParam.getVarType()) {
                return;
            }

            target.setParam(matParam.getName(), matParam.getVarType(), matParam.getValue());
        });

        var additionalRenderState = target.getAdditionalRenderState();
        additionalRenderState.set(source.getAdditionalRenderState());

        target.setKey(source.getKey());
    }

    /**
     * Remove all material parameters with null value for all geometries.
     *
     * @param spatial the model.
     */
    @JmeThread
    public static void cleanUpMaterialParams(@NotNull Spatial spatial) {
        NodeUtils.visitGeometry(spatial, geometry -> {
            var material = geometry.getMaterial();
            if (material != null) {
                cleanUp(material);
            }
        });
    }

    /**
     * Clean up a material.
     *
     * @param material the material.
     */
    @JmeThread
    private static void cleanUp(@NotNull Material material) {
        var params = new ArrayList<MatParam>(material.getParams());
        params.stream().filter(param -> param.getValue() == null)
                .forEach(matParam -> material.clearParam(matParam.getName()));
    }

    /**
     * Save if need textures of a material.
     *
     * @param material the material.
     */
    @FromAnyThread
    public static void saveIfNeedTextures(@NotNull Material material) {
        var params = material.getParams();
        params.stream().filter(matParam -> matParam.getVarType() == VarType.Texture2D)
                .map(MatParam::getValue)
                .map(Texture.class::cast)
                .forEach(MaterialUtils::saveIfNeedTexture);
    }

    /**
     * Save if need a texture.
     *
     * @param texture the texture.
     */
    @FromAnyThread
    private static void saveIfNeedTexture(@NotNull Texture texture) {

        var image = texture.getImage();
        if (!image.isChanged()) {
            return;
        }

        var key = texture.getKey();
        var file = notNull(getRealFile(key.getName()));
        var bufferedImage = ImageToAwt.convert(image, false, true, 0);

        try (var out = Files.newOutputStream(file, WRITE, TRUNCATE_EXISTING, CREATE)) {
            ImageIO.write(bufferedImage, "png", out);
        } catch (IOException e) {
            e.printStackTrace();
        }

        image.clearChanges();
    }

    /**
     * Set the material parameter with check of existing this parameter in the material's definition.
     *
     * @param material the material.
     * @param name     the parameter's name.
     * @param value    the value.
     */
    @FromAnyThread
    public static void safeSet(@Nullable Material material, @NotNull String name, float value) {

        if (material == null) {
            return;
        }

        var materialParam = material.getMaterialDef()
                .getMaterialParam(name);

        if (materialParam != null) {
            material.setFloat(name, value);
        }
    }

    /**
     * Set the material parameter with check of existing this parameter in the material's definition.
     *
     * @param material the material.
     * @param name     the parameter's name.
     * @param value    the value.
     */
    @FromAnyThread
    public static void safeSet(@Nullable Material material, @NotNull String name, boolean value) {

        if (material == null) {
            return;
        }

        var materialParam = material.getMaterialDef()
                .getMaterialParam(name);

        if (materialParam != null) {
            material.setBoolean(name, value);
        }
    }
}
