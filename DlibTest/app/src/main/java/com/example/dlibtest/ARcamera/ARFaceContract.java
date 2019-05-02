package com.example.dlibtest.ARcamera;

import org.reactivestreams.Subscription;
import io.realm.RealmList;

public interface ARFaceContract {
    interface View {
        void onSubscribe(Subscription subscription);
    }

    interface Presenter {
        void resetFaceTexture();
        void swapFace(String swapPath);
        void startFaceScanTask(final RealmList<ImageBean> data);
    }
}
