/*
 * Copyright 2007 Yusuke Yamamoto
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package twitter4j;

import allen.town.focus.twitter.model.Hashtag;

/**
 * A data class representing one single Hashtag entity.
 *
 * @author Yusuke Yamamoto - yusuke at mac.com
 * @since Twitter4J 2.1.9
 */
/*package*/ class HashtagEntityJSONImplMastodon extends EntityIndex implements HashtagEntity, SymbolEntity {
    private static final long serialVersionUID = -5317828991902848906L;
    private String text;
    private String url;


    /* package */ HashtagEntityJSONImplMastodon(Hashtag json) throws Exception {
        super();
        init(json);
    }

    public HashtagEntityJSONImplMastodon(String text, String url) {
        this.text = text;
        this.url = url;
    }

    private void init(Hashtag json) {

        this.text = json.name;
        this.url = json.url;
    }

    @Override
    public String getText() {
        return text;
    }

    @Override
    public int getStart() {
        return super.getStart();
    }

    @Override
    public int getEnd() {
        return super.getEnd();
    }

    @Override
    public String getUrl() {
        return url;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        HashtagEntityJSONImplMastodon that = (HashtagEntityJSONImplMastodon) o;

        if (text != null ? !text.equals(that.text) : that.text != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return text != null ? text.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "HashtagEntityJSONImpl{" +
                "text='" + text + '\'' +
                '}';
    }
}