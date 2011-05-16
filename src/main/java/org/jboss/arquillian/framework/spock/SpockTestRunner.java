/*
 * JBoss, Home of Professional Open Source
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.arquillian.framework.spock;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;

import org.jboss.arquillian.impl.DeployableTestBuilder;
import org.jboss.arquillian.spi.ContainerProfile;
import org.jboss.arquillian.spi.TestResult;
import org.jboss.arquillian.spi.TestResult.Status;
import org.jboss.arquillian.spi.TestRunner;
import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.manipulation.Filter;
import org.junit.runner.notification.RunListener;
import org.junit.runner.notification.RunNotifier;
import org.spockframework.runtime.Sputnik;
import org.spockframework.runtime.model.FeatureInfo;
import org.spockframework.runtime.model.SpecInfo;

/**
 * SpockTestRunner
 *
 * @author <a href="mailto:aslak@redhat.com">Aslak Knutsen</a>
 * @version $Revision: $
 */
public class SpockTestRunner implements TestRunner
{
   /** 
    * Overwrite to provide additional run listeners. 
    */
   protected List<RunListener> getRunListeners()
   {
      return Collections.emptyList();
   }

   protected ContainerProfile getProfile()
   {
      return ContainerProfile.CONTAINER;
   }

   /* (non-Javadoc)
    * @see org.jboss.arquillian.spi.TestRunner#execute(java.lang.Class, java.lang.String)
    */
   @Override
   public TestResult execute(final Class<?> testClass, final String methodName)
   {
      DeployableTestBuilder.setProfile(getProfile());
      
      final Sputnik runner = new Sputnik(testClass);
      try
      {
         runner.filter(new Filter()
         {
            private SpecInfo currentSpec;
            {
               try
               {
                  Field specsField = Sputnik.class.getDeclaredField("spec");
                  specsField.setAccessible(true);
                  currentSpec = (SpecInfo)specsField.get(runner);
               }
               catch (Exception e)
               {
                  throw new RuntimeException("Could not get SpecInfo from Sputnik Runner", e);
               }
            }
            
            @Override
            public boolean shouldRun(Description description)
            {
               for(FeatureInfo feature : currentSpec.getAllFeatures())
               {
                  if(feature.getFeatureMethod().getReflection().getName().equals(methodName))
                  {
                     return true;
                  }
               }
               return false;
            }
            
            @Override
            public String describe()
            {
               return "Filter Feature";
            }
         });
      } 
      catch (Exception e) 
      {
         return new TestResult(Status.FAILED, e);
      }
      
      RunNotifier notifier = new RunNotifier();
      for (RunListener listener : getRunListeners())
         notifier.addListener(listener);

      Result testResult = new Result();
      
      notifier.addFirstListener(testResult.createListener());
      runner.run(notifier);
      DeployableTestBuilder.clearProfile();
      return convertToTestResult(testResult);
   }

   /**
    * Convert a JUnit Result object to Arquillian TestResult
    * 
    * @param result JUnit Test Run Result
    * @return The TestResult representation of the JUnit Result
    */
   private TestResult convertToTestResult(Result result)
   {
      Status status = Status.PASSED;
      Throwable throwable = null;
      if (result.getFailureCount() > 0)
      {
         status = Status.FAILED;
         throwable = result.getFailures().get(0).getException();
      }
      if (result.getIgnoreCount() > 0)
      {
         status = Status.SKIPPED;
      }
      return new TestResult(status, throwable);
   }
}
