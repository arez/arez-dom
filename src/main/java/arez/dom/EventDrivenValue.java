package arez.dom;

import akasha.EventListener;
import akasha.EventTarget;
import arez.ComputableValue;
import arez.Disposable;
import arez.annotations.Action;
import arez.annotations.ArezComponent;
import arez.annotations.ComputableValueRef;
import arez.annotations.DepType;
import arez.annotations.Feature;
import arez.annotations.Memoize;
import arez.annotations.Observable;
import arez.annotations.OnActivate;
import arez.annotations.OnDeactivate;
import java.util.Objects;
import javax.annotation.Nonnull;
import jsinterop.annotations.JsFunction;

/**
 * An observable component that exposes a value provided by a lambda as observable where the value can
 * change in response to a browser event. A typical example is making the value of <code>window.innerWidth</code>
 * observable by listening to <code>"resize"</code> events on the window. This could be achieved with code such
 * as:
 *
 * <pre>{@code
 * EventDrivenValue<Window, Integer> innerWidth = EventDrivenValue.create( window, "resize", () -> window.innerWidth )
 * }</pre>
 *
 * <p>It is important that the code not add a listener to the underlying event source until there is an
 * observer accessing the <code>"value"</code> observable defined by the EventDrivenValue class. The first
 * observer that observes the observable will result in an event listener being added to the event source
 * and this listener will not be removed until there is no observers left observing the value. This means
 * that a component that is not being used has very little overhead.</p>
 *
 * @param <SourceType> the type of the DOM element that generates events of interest.
 * @param <ValueType>  the type of the value returned by the "value" observable.
 */
@ArezComponent( requireId = Feature.DISABLE, disposeNotifier = Feature.DISABLE )
public abstract class EventDrivenValue<SourceType extends EventTarget, ValueType>
{
  /**
   * The functional interface defining accessor.
   *
   * @param <SourceType> the type of the DOM element that generates events of interest.
   * @param <ValueType>  the type of the value returned by the "value" observable.
   */
  @FunctionalInterface
  @JsFunction
  public interface Accessor<SourceType extends EventTarget, ValueType>
  {
    /**
     * Return the value.
     *
     * @param source the source that drives the access.
     * @return the value
     */
    ValueType get( @Nonnull SourceType source );
  }

  /**
   * The
   */
  @Nonnull
  private final EventListener _listener = e -> onEvent();
  @Nonnull
  private SourceType _source;
  @Nonnull
  private final String _event;
  @Nonnull
  private final Accessor<SourceType, ValueType> _getter;
  private boolean _active;

  /**
   * Create the component.
   *
   * @param <SourceType> the type of the DOM element that generates events of interest.
   * @param <ValueType>  the type of the value returned by the "value" observable.
   * @param source       the DOM element that generates events of interest.
   * @param event        the event type that could result in changes to the observed value. The event type is expected to be generated by the source element.
   * @param getter       the function that retrieves the observed value from the platform.
   * @return the new component.
   */
  @Nonnull
  public static <SourceType extends EventTarget, ValueType>
  EventDrivenValue<SourceType, ValueType> create( @Nonnull final SourceType source,
                                                  @Nonnull final String event,
                                                  @Nonnull final Accessor<SourceType, ValueType> getter )
  {
    return new Arez_EventDrivenValue<>( source, event, getter );
  }

  EventDrivenValue( @Nonnull final SourceType source,
                    @Nonnull final String event,
                    @Nonnull final Accessor<SourceType, ValueType> getter )
  {
    _source = Objects.requireNonNull( source );
    _event = Objects.requireNonNull( event );
    _getter = Objects.requireNonNull( getter );
  }

  /**
   * Return the element that generates the events that report potential changes to the observed value.
   *
   * @return the associated element.
   */
  @Nonnull
  @Observable
  public SourceType getSource()
  {
    return _source;
  }

  /**
   * Set the element that generates events.
   * This ensures that the event listeners are managed correctly if the source is currently being observed.
   *
   * @param source the the event source.
   */
  public void setSource( @Nonnull final SourceType source )
  {
    if ( _active )
    {
      unbindListener();
    }
    _source = source;
    if ( _active )
    {
      bindListener();
    }
  }

  /**
   * Return the value.
   *
   * @return the value.
   */
  @Memoize( depType = DepType.AREZ_OR_EXTERNAL )
  public ValueType getValue()
  {
    // Deliberately observing source via getSource() so that this method re-runs
    // when source changes
    return _getter.get( getSource() );
  }

  @ComputableValueRef
  abstract ComputableValue<?> getValueComputableValue();

  /**
   * Hook invoked when the value moves from unobserved to observed.
   * Adds underlying listener.
   */
  @OnActivate
  void onValueActivate()
  {
    _active = true;
    bindListener();
  }

  /**
   * Hook invoked when value is no longer observed.
   * Removes underlying listener.
   */
  @OnDeactivate
  void onValueDeactivate()
  {
    _active = false;
    unbindListener();
  }

  private void onEvent()
  {
    // Due to bugs (?) or perhaps "implementation choices" in some browsers, an event can be delivered
    // after listener is removed. According to notes in https://github.com/ReactTraining/react-media/blob/master/modules/MediaQueryList.js
    // Safari doesn't clear up listener queue on MediaQueryList when removeListener is called if there
    // is already waiting in the internal event queue.
    //
    // To avoid a potential crash when invariants are enabled or indeterminate behaviour when invariants
    // are not enabled, a guard has been added.
    if ( Disposable.isNotDisposed( this ) )
    {
      notifyOnChange();
    }
  }

  /**
   * Hook invoked from listener to indicate  memoized value should be recomputed.
   */
  @Action
  void notifyOnChange()
  {
    getValueComputableValue().reportPossiblyChanged();
  }

  /**
   * Add underlying listener to source.
   */
  private void bindListener()
  {
    _source.addEventListener( _event, _listener );
  }

  /**
   * Remove underlying listener from source.
   */
  private void unbindListener()
  {
    _source.removeEventListener( _event, _listener );
  }
}
