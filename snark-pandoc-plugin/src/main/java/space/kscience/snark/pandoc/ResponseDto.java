package space.kscience.snark.pandoc;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response from github/releases/latest
 */
public class ResponseDto {

    public AssetDto[] getAssets() {
        return assets;
    }

    public void setAssets(AssetDto[] assets) {
        this.assets = assets;
    }

    /**
     * @param osSuffix
     * @return asset appropriate to os
     */
    public AssetDto getAssetByOsSuffix(String osSuffix) {
        for (var asset : assets) {
            if (asset.getName().contains(osSuffix)) {
                return asset;
            }
        }
        throw new IllegalArgumentException("Unexpected osSuffix");
    }


    public static class AssetDto {

        @JsonProperty("browser_download_url")
        private String browserDownloadUrl;
        private String name;

        public String getBrowserDownloadUrl() {
            return browserDownloadUrl;
        }

        public void setBrowserDownloadUrl(String browserDownloadUrl) {
            this.browserDownloadUrl = browserDownloadUrl;
        }

        public AssetDto() {}

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
    private AssetDto[] assets;

    public String getTagName() {
        return tagName;
    }

    public void setTagName(String tagName) {
        this.tagName = tagName;
    }

    @JsonProperty("tag_name")
    private String tagName;

    public ResponseDto() {}
}
