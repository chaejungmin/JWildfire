/*
  JWildfire - an image and animation processor written in Java 
  Copyright (C) 1995-2011 Andreas Maschke

  This is free software; you can redistribute it and/or modify it under the terms of the GNU Lesser 
  General Public License as published by the Free Software Foundation; either version 2.1 of the 
  License, or (at your option) any later version.
 
  This software is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without 
  even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU 
  Lesser General Public License for more details.

  You should have received a copy of the GNU Lesser General Public License along with this software; 
  if not, write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
  02110-1301 USA, or see the FSF site: http://www.fsf.org.
*/
package org.jwildfire.create.tina.variation;

import org.jwildfire.create.tina.base.XForm;
import org.jwildfire.create.tina.base.XYZPoint;
import org.jwildfire.create.tina.random.RandomNumberGenerator;

public class Pie3DFunc extends VariationFunc {

  private static final String PARAM_SLICES = "slices";
  private static final String PARAM_ROTATION = "rotation";
  private static final String PARAM_THICKNESS = "thickness";
  private static final String[] paramNames = { PARAM_SLICES, PARAM_ROTATION, PARAM_THICKNESS };

  private double slices = 7;
  private double rotation = 0.0;
  private double thickness = 0.5;

  @Override
  public void transform(TransformationContext pContext, XForm pXForm, XYZPoint pAffineTP, XYZPoint pVarTP, double pAmount) {
    RandomNumberGenerator randGen = pContext.getRandomNumberGenerator();
    int sl = (int) (randGen.random() * slices + 0.5);
    double a = rotation + 2.0 * Math.PI * (sl + randGen.random() * thickness) / slices;
    double r = pAmount * randGen.random();
    double sina = Math.sin(a);
    double cosa = Math.cos(a);
    pVarTP.x += r * cosa;
    pVarTP.y += r * sina;
    pVarTP.z += r * Math.sin(r);
  }

  @Override
  public String[] getParameterNames() {
    return paramNames;
  }

  @Override
  public Object[] getParameterValues() {
    return new Object[] { slices, rotation, thickness };
  }

  @Override
  public void setParameter(String pName, double pValue) {
    if (PARAM_SLICES.equalsIgnoreCase(pName))
      slices = pValue;
    else if (PARAM_ROTATION.equalsIgnoreCase(pName))
      rotation = pValue;
    else if (PARAM_THICKNESS.equalsIgnoreCase(pName))
      thickness = pValue;
    else
      throw new IllegalArgumentException(pName);
  }

  @Override
  public String getName() {
    return "pie3D";
  }

}
