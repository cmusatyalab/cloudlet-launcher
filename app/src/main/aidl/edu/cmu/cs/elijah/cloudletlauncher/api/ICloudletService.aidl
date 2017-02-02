// ICloudletService.aidl
package edu.cmu.cs.elijah.cloudletlauncher.api;

import edu.cmu.cs.elijah.cloudletlauncher.api.ICloudletServiceCallback;

interface ICloudletService {
    String findCloudlet();

    void connectVPN();

    void disconnectVPN();

    void registerCallback(ICloudletServiceCallback callback);

    void unregisterCallback(ICloudletServiceCallback callback);
}
