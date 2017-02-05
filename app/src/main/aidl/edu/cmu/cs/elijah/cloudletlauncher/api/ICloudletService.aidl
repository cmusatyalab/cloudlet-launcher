// ICloudletService.aidl
package edu.cmu.cs.elijah.cloudletlauncher.api;

import edu.cmu.cs.elijah.cloudletlauncher.api.ICloudletServiceCallback;

interface ICloudletService {
    void findCloudlet();

    void disconnectCloudlet();

    void registerCallback(ICloudletServiceCallback callback);

    void unregisterCallback(ICloudletServiceCallback callback);
}
