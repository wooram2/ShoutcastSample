package com.example.nhnent.shoutcastplayerapp;

/**
 * Created by Wooram2 on 2017. 7. 31..
 */

import android.util.Log;

import com.google.android.exoplayer2.upstream.DataSpec;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource;
import com.google.android.exoplayer2.upstream.TransferListener;
import com.google.android.exoplayer2.util.Predicate;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

public class IcyDataSource extends DefaultHttpDataSource{

    private final String TAG = IcyDataSource.class.getSimpleName();

    private int mPeriod;

    private int mRemaining;

    private byte[] mBuffer;

    private boolean mInvalidIcyMeta;

    public IcyDataSource(String userAgent, Predicate<String> contentTypePredicate,
                         TransferListener<? super DefaultHttpDataSource> listener, int connectTimeoutMillis,
                         int readTimeoutMillis, boolean allowCrossProtocolRedirects,
                         RequestProperties defaultRequestProperties) {

        super(userAgent, null,
                listener, connectTimeoutMillis,
                readTimeoutMillis, allowCrossProtocolRedirects,
                defaultRequestProperties);

        mBuffer = new byte[128];
    }

    @Override
    public long open(DataSpec dataSpec) throws HttpDataSourceException {

        setRequestProperty("Icy-MetaData", "1");

        long ret = super.open(dataSpec);

        String icyMetaInt = getIcyMetaInt();

        if (icyMetaInt != null) {
            mPeriod = Integer.valueOf(icyMetaInt);
            mRemaining = mPeriod;
            mInvalidIcyMeta = false;
        } else {
            mInvalidIcyMeta = true;
        }

        return ret;
    }

    private String getIcyMetaInt() {
        Map<String, List<String>> headers = getResponseHeaders();
        List<String> values = headers.get("icy-metaint");
        String icyMetaInt = values.get(0);
        return icyMetaInt;
    }

    @Override
    public int read(byte[] buffer, int offset, int readLength) throws HttpDataSourceException {

        if (mInvalidIcyMeta) {
            return super.read(buffer, offset, mRemaining < readLength ? mRemaining : readLength);
        } else {

            int ret = super.read(buffer, offset, mRemaining < readLength ? mRemaining : readLength);

            if (mRemaining == ret) {
                try {
                    fetchMetadata();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                mRemaining -= ret;
            }

            return ret;
        }
    }

    private void fetchMetadata() throws IOException {

        mRemaining = mPeriod;

        InputStream in = getConnection().getInputStream();

        int metaSize = in.read();

        if (metaSize < 1) { return; }

        metaSize <<= 4; // multiplied by 16.

        if (mBuffer.length < metaSize) {
            mBuffer = null;
            mBuffer = new byte[metaSize];
        }

        metaSize = readFully(mBuffer, 0, metaSize, in);

        String meta = new String(mBuffer, 0, metaSize, "UTF-8");

        /**
         * 'http://24sky.saycast.com'에서 메타를 출력하면, value 값이 깨져보입니다.
         * value에서 사용되는 문자 인코딩이 euc-kr이기 때문입니다.
         */
        Log.i(TAG, "metadata : " + meta); // 메타데이터를 출력합니다.
    }

    private int readFully(byte[] buffer, int offset, int size, InputStream in) throws IOException {

        int n;
        int oo = offset;

        while(size > 0 && (n = in.read(buffer, offset, size )) != -1) {
            offset += n;
            size -= n;
        }

        return offset - oo;
    }
}
