function [lat, lon] = groundTrack(sa, ecc, inc, raan, argp, ta, t)
%computes the ground track of a satellite in an orbit around the
%Earth while accounting for nodal precession. Semi-major axis and
%incliantion are fixed. 
%INPUT (vector of values for each parameter)
%   sa: semi-major axis[m] 
%   ecc: eccentricity
%   inc: inclination [deg]
%   raan: initial right ascension of the ascending node [deg]
%   argp: argument of perigee [deg]
%   ta: initial true anomaly [deg]
%   t: time vector [s]
%
%OUTPUT
%   lat: latitudes [deg]
%   lon: longitudes [deg]

%TODO: currently assumes satellite starts at (r,0,0) in the fixed inertial
%frame

[x,y,z] = orbitalElem2xyz(sa, ecc, inc, raan, argp, ta, t);

lat = asind(z./sa);
lon = acosd(x./(sa.*cosd(lat)));
lon = lon.*sign(y);