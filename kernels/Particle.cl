#pragma OPENCL EXTENSION cl_khr_fp64 : enable

constant float PI_6 = (float)(M_PI/6);
constant float PI_3 = (float)(M_PI/3);
constant float PI_2 = (float)(M_PI_2);
constant float _2PI_3 = (float)(2*M_PI/3);
constant float _5PI_6 = (float)(5*M_PI/6);

inline float getLonDelta(float lon, float delta) {
    if (lon + delta > M_PI) {
        delta -= 2*M_PI;
    } else if (lon < -M_PI) {
        delta += 2*M_PI;
    }
    return delta;
}

inline float getLatDelta(float lat, float delta) {
    if (lat + delta > M_PI) {
        return 0;
    } else if (lat + delta < 0) {
        return 0;
    }
    return delta;
}

inline float cartesianToLongitude(float x, float z) {
    return atan2(z, x);
}

inline float cartesianToLatitude(float x, float y, float z) {
    return acos(y / sqrt(x*x + y*y + z*z));
}

inline float* sphericalToCartesian(float latitude, float longitude, float radius) {
    float coords[3] = {
        (radius * sin(latitude) * cos(longitude)),
        (radius * cos(latitude)),
        (radius * sin(latitude) * sin(longitude))
    };
    return coords;
}

__kernel void update(__global float* wx, __global float* wy, __global float* wz, 
                     __global float* wt, __global float* we, __global int* ua,
                     const int frame) 
{
    int gid = get_global_id(0);
    float PlanetRadius = 10.0;

    float lat = cartesianToLatitude(wx[gid], wy[gid], wz[gid]);
    float lon = cartesianToLongitude(wx[gid], wz[gid]);

    float percentToBoundary;
    float lonDelta = .005;
    float latDelta = .001;

    float seasonOffset = sin(((float)frame / 3599.0) * 2 * M_PI);
    float equatorOffset = seasonOffset * 0.40904;

    // Change position based on layer, belt, coriolis force
    if (lat < PI_6+equatorOffset)
    {
        percentToBoundary = lat / PI_6;
        lon += (ua[gid] == 1)
                ? getLonDelta(lon, lonDelta * percentToBoundary)
                : getLonDelta(lon, -lonDelta * (1 - percentToBoundary));

        lat += (ua[gid] == 1)
                ? getLatDelta(lat, latDelta)
                : getLatDelta(lat, -latDelta);
    }
    else if (lat < PI_3+equatorOffset)
    {
        percentToBoundary = (lat - PI_6) / (PI_3 - PI_6);
        lon += (ua[gid] == 1)
                ? getLonDelta(lon, -lonDelta * (1 - percentToBoundary))
                : getLonDelta(lon, lonDelta * percentToBoundary);

        lat += (ua[gid] == 1)
                ? getLatDelta(lat, -latDelta)
                : getLatDelta(lat, latDelta);
    }
    else if (lat < PI_2+equatorOffset)
    {
        percentToBoundary = (lat - PI_3) / (PI_2 - PI_3);
        lon += (ua[gid] == 1)
                ? getLonDelta(lon, lonDelta * percentToBoundary)
                : getLonDelta(lon, -lonDelta * (1 - percentToBoundary));

        lat += (ua[gid] == 1)
                ? getLatDelta(lat, latDelta)
                : getLatDelta(lat, -latDelta);
    }
    else if (lat < _2PI_3+equatorOffset)
    {
        percentToBoundary = (lat - PI_2) / (_2PI_3 - PI_2);
        lon += (ua[gid] == 1)
                ? getLonDelta(lon, lonDelta * (1 - percentToBoundary))
                : getLonDelta(lon, -lonDelta * percentToBoundary);

        lat += (ua[gid] == 1)
                ? getLatDelta(lat, -latDelta)
                : getLatDelta(lat, latDelta);
    }
    else if (lat < _5PI_6+equatorOffset)
    {
        percentToBoundary = (lat - _2PI_3) / (_5PI_6 - _2PI_3);
        lon += (ua[gid] == 1)
                ? getLonDelta(lon, -lonDelta * percentToBoundary)
                : getLonDelta(lon, lonDelta * (1 - percentToBoundary));

        lat += (ua[gid] == 1)
                ? getLatDelta(lat, latDelta)
                : getLatDelta(lat, -latDelta);
    }
    else
    {
        percentToBoundary = (lat - _5PI_6) / (float)(M_PI - _5PI_6);
        lon += (ua[gid] == 1)
                ? getLonDelta(lon, lonDelta * (1 - percentToBoundary))
                : getLonDelta(lon, -lonDelta * percentToBoundary);

        lat += (ua[gid] == 1)
                ? getLatDelta(lat, -latDelta)
                : getLatDelta(lat, latDelta);
    }

    // Change temperature accordingly
    wt[gid] += (ua[gid] == 1) ? -1 : 1;

    // Change elevation based on temp
    we[gid] += (wt[gid] + 50) * 0.2;

    // Change layer if elevation passes threshold
    ua[gid] = (we[gid] >= 7000) ? 1 : 0;

    // Convert back to cartesian coordinates and update
    float* position = sphericalToCartesian(lat, lon, PlanetRadius);
    wx[gid] = position[0];
    wy[gid] = position[1];
    wz[gid] = position[2];
}