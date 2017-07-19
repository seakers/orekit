function [x,y,z] = orbitalElem2xyz(sa, ecc, inc, raan, argp, ta, t)
%computes the x,y,z position in the rotating earth frame of a satellite in an orbit around the
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
%%OUTPUT
%   x: x-position [m] (in rotating earth frame)
%   y: y-position [m] (in rotating earth frame)
%   z: z-position [m] (in rotating earth frame)

%TODO: currently assumes satellite starts at (r,0,0) in the rotating earth
%frame

%fix all vectors to be column vectors
[a,b] = size(sa);
if(a<b)
    sa = sa';
end
[a,b] = size(ecc);
if(a<b)
    ecc = ecc';
end
[a,b] = size(inc);
if(a<b)
    inc = inc';
end
[a,b] = size(raan);
if(a<b)
    raan = raan';
end
[a,b] = size(argp);
if(a<b)
    argp = argp';
end
[a,b] = size(ta);
if(a<b)
    ta = ta';
end
[a,b] = size(t);
if(a<b)
    t = t';
end
    
x = (sa.*(1-ecc.^2)./(1+ecc.*cosd(ta))).*(cosd(raan).*cosd(argp+ta)-sind(raan).*sind(argp+ta).*cosd(inc));
y = (sa.*(1-ecc.^2)./(1+ecc.*cosd(ta))).*(sind(raan).*cosd(argp+ta)+cosd(raan).*sind(argp+ta).*cosd(inc));
z = (sa.*(1-ecc.^2)./(1+ecc.*cosd(ta))).*(sind(argp+ta).*sind(inc));