function [lat, lon] = approxGroundTrack(a, inc, raan, ta, t)
%computes the ground track of a satellite in a circular orbit around the
%Earth while accounting for nodal precession. Semi-major axis and
%incliantion are fixed. 
%INPUT
%   a: semi-major axis[m] 
%   inc: inclination [deg]
%   raan: initial right ascension of the ascending node [deg]
%   ma: initial true anomaly [deg]
%   t: time vector [s]
%
%OUTPUT
%   lat: latitudes [deg]
%   lon: longitudes [deg]

%TODO: currently assumes satellite starts at (r,0,0) in the fixed inertial
%frame

%constants
re = 6378137;
mu = 3.986004418*10^14;
j2 = 1.08262668*10^(-3);

%satellite movement 
ta_dot = 1/(sqrt(a^3/mu))*180/pi;
ta_t = ta+ta_dot.*t;

%nodal precession
raan_dot = (-1.5*re^2/a^2*j2*(1/sqrt(a^3/mu))*cosd(inc))*180/pi;
raan_t = raan+(raan_dot-(360/86400)).*t;

%other elements
sa = ones(length(t),1)*a;
ecc = zeros(length(t),1);
incl = ones(length(t),1)*inc;
argp = zeros(length(t),1);

[lat,lon] = groundTrack(sa,ecc,incl,raan_t,argp,ta_t,t);