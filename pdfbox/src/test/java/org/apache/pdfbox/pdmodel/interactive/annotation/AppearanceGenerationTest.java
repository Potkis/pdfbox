/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.pdfbox.pdmodel.interactive.annotation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.contentstream.operator.Operator;
import org.apache.pdfbox.cos.COSFloat;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdfparser.PDFStreamParser;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.rendering.TestPDFToImage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/*
 * Tests the appearance generation for annotations generated using
 * Adobe Reader DC.
 * 
 * - gets the annotation
 * - gets the appearance stream and it's tokens
 * - removes the appearance stream
 * - regenerates the appearance
 * - compares the tokens from the original to the ones created by PDFBox
 * 
 * For the initial work only the token operators are compared to ensure the 
 * same basic operation. Upon refinement the operators parameters could also be
 * verified.
 * 
 */
public class AppearanceGenerationTest
{

    // delta for comparing equality of float values
    // a difference in float values smaller than this
    // will be treated equal between Adobe and PDFBox 
    // values.
    // TODO: revisit that number as our code improves
    private static final float DELTA = 3e-3f;
    
    // the location of the annotation
    static PDRectangle rectangle;
    
    private PDDocument document;
    
    private static final File IN_DIR = new File("src/test/resources/org/apache/pdfbox/pdmodel/interactive/annotation");
    private static final File OUT_DIR = new File("target/test-output/pdmodel/interactive/annotation");
    private static final String NAME_OF_PDF = "AnnotationTypes.pdf";
    
    @BeforeEach
    public void setUp() throws IOException
    {
        document = Loader.loadPDF(new File(IN_DIR, NAME_OF_PDF));
        OUT_DIR.mkdirs();
    }
    
    // Test currently disabled as the content stream differs
    @Test
    public void rectangleFullStrokeNoFill() throws IOException
    {
        PDPage page = document.getPage(0);
        
        PDAnnotation annotation = page.getAnnotations().get(0);
        
        // get the tokens of the content stream generated by Adobe
        PDAppearanceStream appearanceContentStream = annotation.getNormalAppearanceStream();
        PDFStreamParser streamParser = new PDFStreamParser(appearanceContentStream);
        
        List<Object> tokensForOriginal = streamParser.parse();
                
        // get the tokens for the content stream generated by PDFBox
        annotation.getCOSObject().removeItem(COSName.AP);
        annotation.constructAppearances();
        
        appearanceContentStream = annotation.getNormalAppearanceStream();
        streamParser = new PDFStreamParser(appearanceContentStream);
        
        List<Object> tokensForPdfbox = streamParser.parse();
        
        assertEquals(tokensForOriginal.size(), tokensForPdfbox.size(),
                "The number of tokens in the content stream should be the same");
        
        int actualToken = 0;
        for (Object tokenForOriginal : tokensForOriginal)
        {
            Object tokenForPdfbox = tokensForPdfbox.get(actualToken);
            assertEquals(tokenForOriginal.getClass().getName(), tokenForPdfbox.getClass().getName(),
                    "The tokens should have the same type");
            
            if (tokenForOriginal instanceof Operator)
            {
                assertEquals(((Operator) tokenForOriginal).getName(),
                        ((Operator) tokenForPdfbox).getName(),
                        "The operator generated by PDFBox should be the same Operator");
            } else if (tokenForOriginal instanceof COSFloat)
            {
                assertTrue(
                        Math.abs(((COSFloat) tokenForOriginal).floatValue()
                                - ((COSFloat) tokenForPdfbox).floatValue()) < DELTA,
                        "The difference between the numbers should be smaller than " + DELTA);
            }
            actualToken++;
        }
        
        // Save the file for manual comparison for now
        File file = new File(OUT_DIR, NAME_OF_PDF + "-newAP.pdf");
        document.save(file);
    }
    
    
    // we should render similar to Adobe Reader using the original file
    @Test
    public void renderTest() throws IOException
    {
        File file = new File(OUT_DIR, NAME_OF_PDF);
        document.save(file);
        // compare rendering
        TestPDFToImage testPDFToImage = new TestPDFToImage(TestPDFToImage.class.getName());
        if (!testPDFToImage.doTestFile(file, IN_DIR.getAbsolutePath(), OUT_DIR.getAbsolutePath()))
        {
            // don't fail, rendering is different on different systems, result must be viewed manually
            System.out.println("Rendering of " + file + " failed or is not identical to expected rendering in " + IN_DIR + " directory");
        }
    }
    
    @AfterEach
    public void tearDown() throws IOException
    {
        document.close();
    }
}
