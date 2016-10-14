from mpl_toolkits.basemap import Basemap
import numpy as np
import matplotlib.pyplot as plt
import scipy.interpolate

# read in topo data (on a regular lat/lon grid)
gapData=np.loadtxt('gaps.txt',delimiter=',',skiprows=1)
lats = gapData[:,0]*180/np.pi
lons = gapData[:,1]*180/np.pi

gaps = gapData[:,2]
# create Basemap instance for Robinson projection.
m = Basemap(projection='kav7',lon_0=0)

#interpolate 
latsi,lonsi = np.linspace(lats.min(),lats.min(),10),np.linspace(lons.min(),lons.min(),10)
latsi,lonsi = np.meshgrid(latsi,lonsi)
gapsi = scipy.interpolate.griddata((lats,lons), gaps, (latsi,lonsi), method='nearest')

# make filled contour plot.
im1 = m.contourf(lonsi,latsi,gapsi) 
# draw coastlines.
m.drawcoastlines()
# draw parallels and meridians.
m.drawparallels(np.arange(-60.,90.,30.),labels=[1,0,0,0])
m.drawmeridians(np.arange(0.,360.,60.),labels=[0,0,0,1],fontsize=12)
m.colorbar(location='bottom',pad='10%')
# add a title.
plt.title('Robinson Projection')
plt.show()
         
