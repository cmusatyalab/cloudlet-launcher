// ICloudletService.aidl
package edu.cmu.cs.elijah.cloudletlauncher.api;

import edu.cmu.cs.elijah.cloudletlauncher.api.ICloudletServiceCallback;

interface ICloudletService {
    boolean isServiceReady();

    String getVpnProfileUuid();

    void useTestProfile(boolean flag);

    void setUserId(String userId);

    void startOpenVpn();

    void endOpenVpn();

    void findCloudlet(String appId);

    void disconnectCloudlet(String appId);

    void registerCallback(ICloudletServiceCallback callback);

    void unregisterCallback(ICloudletServiceCallback callback);
}
