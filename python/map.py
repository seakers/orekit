# This python script reads in a csv file where each line in the
# file contains the latitude [deg], longitude [deg], and a vector of
# metrics to plot on the map.
#
#REQUIRES PYTHON 3

import matplotlib.pyplot as plt
import numpy as np
import csv
from mpl_toolkits.basemap import Basemap


# initialize the results
results = {}

# load in metrics file
with open('test.csv', newline='') as csvfile:
    reader = csv.reader(csvfile, delimiter=',')
    for row in reader:
        if row[0].startswith('#'):
            continue
        lat = float(row[0])
        lon = float(row[1])
        metrics = row[2:-1]
        for m in metrics:
            if lat not in results:
                results[lat] = {}
            if lon not in results[lat]:
                results[lat][lon] = []
            results[lat][lon].append(float(m))

# check that each latitude has the same number of longitude
nlats = len(results)
nlons = len(results[lat])
for lat in results.keys():
    if len(results[lat]) != nlons:
        raise Exception('Expected all latitudes to have '\
                        'the same number of longitude '\
                        'points ({0}). Found {1} points '\
                        'for latitude at {2}.'.format(nlons,len(results[lat]),lat))

# create mesh of lat and lon
lats = np.zeros(nlats)
lons = np.zeros(nlons)
for lat_i, key in enumerate(results.keys()):
    lats[lat_i] = key
for lon_i, key in enumerate(results[lats[lat_i]].keys()):
    lons[lon_i] = key
#sort lat and lons in ascending order
lats = np.sort(lats)
lons = np.sort(lons)
#need to copy the left longitudes to the right so that mesh completes
lons = np.append(lons,lons[0]+360)
nlons = nlons + 1

lats_mesh = np.zeros((nlons,nlats))
lons_mesh = np.zeros((nlons,nlats))
metric = np.zeros((nlons,nlats))
for lat_i, lat in enumerate(lats):
    for lon_i, lon in enumerate(lons):
        lats_mesh[lon_i,lat_i] = lat
        lons_mesh[lon_i,lat_i] = lon
        # if the lon doesn't exist, then it must be the lon that was wrapped around
        if not (lon in results[lat]):
            lon = lon - 360
        if np.isnan(results[lat][lon][0]):
            metric[lon_i, lat_i] = 100 / 3600.
        else:
            metric[lon_i, lat_i] = results[lat][lon][1]/ 3600.

# llcrnrlat,llcrnrlon,urcrnrlat,urcrnrlon
# are the lat/lon values of the lower left and upper right corners
# of the map.
# resolution = 'c' means use crude resolution coastlines.
m = Basemap(projection='cyl',llcrnrlat=-90,urcrnrlat=90,\
            llcrnrlon=-180,urcrnrlon=180,resolution='c')
m.drawcoastlines()
# draw parallels and meridians.
m.drawparallels(np.arange(-90.,91.,30.))
m.drawmeridians(np.arange(-180.,181.,60.))

x, y = m(lons_mesh,lats_mesh)
# contour data over the map.
cs = m.contourf(x, y, metric)
cbar = m.colorbar(cs,location='bottom',pad="5%")
cbar.set_label('metric')

#display map
plt.title("Equidistant Cylindrical Projection")
plt.show()