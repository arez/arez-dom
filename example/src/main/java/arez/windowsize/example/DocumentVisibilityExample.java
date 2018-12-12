package arez.windowsize.example;

import arez.Arez;
import arez.windowsize.DocumentVisibility;
import com.google.gwt.core.client.EntryPoint;
import elemental2.dom.DomGlobal;

public class DocumentVisibilityExample
  implements EntryPoint
{
  public void onModuleLoad()
  {
    final DocumentVisibility v = DocumentVisibility.create();
    Arez.context().observer( () -> DomGlobal.console.log( "Document Visibility: " + v.getVisibility() ) );
  }
}
