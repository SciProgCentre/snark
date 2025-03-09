package space.kscience.snark.pandoc

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Response from github/releases/latest
 */
@Serializable
internal class ResponseDto(
    val assets: Array<AssetDto>,
    @SerialName("tag_name") val tagName: String,
) {
    /**
     * @param osSuffix
     * @return asset appropriate to os
     */
    fun getAssetByOsSuffix(osSuffix: String?): AssetDto {
        for (asset in assets) {
            if (asset.name.contains(osSuffix!!)) {
                return asset
            }
        }
        throw IllegalArgumentException("Unexpected osSuffix")
    }


    @Serializable
    public class AssetDto(
        @SerialName("browser_download_url") val browserDownloadUrl: String,
        val name: String
    )

}
