/******************************************************************************
 * Copyright (c) 2000-2018 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 *
 * Contributors:
 *   
 *   Keremi, Andras
 *   Eros, Levente
 *   Kovacs, Gabor
 *
 ******************************************************************************/

package org.eclipse.titan.codegenerator.TTCN3JavaAPI;

import java.util.ArrayList;
import java.util.List;

public abstract class RecordDef extends StructuredTypeDef {
    public List<String> fieldsInOrder; //stores the order of fields of record type
    
    public RecordDef(){
    	fieldsInOrder = new ArrayList<String>();
    }
    
}