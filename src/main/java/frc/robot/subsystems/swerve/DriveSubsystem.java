// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot.subsystems.swerve;

import java.io.IOException;
import java.util.function.DoubleSupplier;

import org.photonvision.PhotonCamera;
import org.photonvision.PhotonPoseEstimator;
import org.photonvision.PhotonPoseEstimator.PoseStrategy;

import com.kauailabs.navx.frc.AHRS;
import com.pathplanner.lib.auto.AutoBuilder;

import edu.wpi.first.apriltag.AprilTagFieldLayout;
import edu.wpi.first.apriltag.AprilTagFields;
import edu.wpi.first.math.VecBuilder;
import edu.wpi.first.math.controller.PIDController;
import edu.wpi.first.math.estimator.SwerveDrivePoseEstimator;
import edu.wpi.first.math.filter.SlewRateLimiter;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Transform3d;
import edu.wpi.first.math.kinematics.ChassisSpeeds;
import edu.wpi.first.math.kinematics.SwerveDriveKinematics;
import edu.wpi.first.math.kinematics.SwerveDriveOdometry;
import edu.wpi.first.math.kinematics.SwerveModulePosition;
import edu.wpi.first.math.kinematics.SwerveModuleState;
import edu.wpi.first.math.util.Units;
import edu.wpi.first.util.sendable.Sendable;
import edu.wpi.first.wpilibj.ADIS16470_IMU;
import edu.wpi.first.wpilibj.SPI;
import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.I2C;
import edu.wpi.first.wpilibj.DriverStation.Alliance;
import edu.wpi.first.wpilibj.shuffleboard.Shuffleboard;
import edu.wpi.first.wpilibj.shuffleboard.ShuffleboardTab;
import edu.wpi.first.wpilibj.smartdashboard.Field2d;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import frc.robot.Bindings;
import frc.robot.Constants;

import frc.robot.Constants.AutoConstants;
import frc.robot.Constants.DriveConstants;
import frc.robot.Constants.FieldConstants;
import frc.robot.util.LookupTables;
import frc.robot.util.PoseMath;
import frc.robot.util.Vision;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.SubsystemBase;


public class DriveSubsystem extends SubsystemBase {
  //Create Slew rate limiters
  private final SlewRateLimiter xLimiter, yLimiter, turningLimiter;
  // Create MAXSwerveModules
  private final MAXSwerveModule m_frontLeft = new MAXSwerveModule(
      DriveConstants.kFrontLeftDrivingCanId,
      DriveConstants.kFrontLeftTurningCanId,
      DriveConstants.kFrontLeftChassisAngularOffset,
      false
      );

  private final MAXSwerveModule m_frontRight = new MAXSwerveModule(
      DriveConstants.kFrontRightDrivingCanId,
      DriveConstants.kFrontRightTurningCanId,
      DriveConstants.kFrontRightChassisAngularOffset,
      false
      );

  private final MAXSwerveModule m_rearLeft = new MAXSwerveModule(
      DriveConstants.kRearLeftDrivingCanId,
      DriveConstants.kRearLeftTurningCanId,
      DriveConstants.kBackLeftChassisAngularOffset,
      false
      );

  private final MAXSwerveModule m_rearRight = new MAXSwerveModule(
      DriveConstants.kRearRightDrivingCanId,
      DriveConstants.kRearRightTurningCanId,
      DriveConstants.kBackRightChassisAngularOffset,
      false
      );

  

  private final PIDController m_VisionLockController = new PIDController(0.014, 0, 0);
  // The gyro sensor
  private final AHRS m_gyro = new AHRS(SPI.Port.kMXP, (byte) 200);

  // Odometry class for tracking robot pose
      private final SwerveDrivePoseEstimator m_poseEstimator =
      new SwerveDrivePoseEstimator(
          DriveConstants.kDriveKinematics,
          Rotation2d.fromDegrees(getHeadingDegrees() ),
          new SwerveModulePosition[] {
            m_frontLeft.getPosition(),
            m_frontRight.getPosition(),
            m_rearLeft.getPosition(),
            m_rearRight.getPosition()
          },
          new Pose2d(0, 0, new Rotation2d(0)), 
          VecBuilder.fill(0.85, 0.85, Units.degreesToRadians(0.5)), // initiial was 0.05 for both on top and 0.5 for bottom, 0.05, 0.05, 0.65
          VecBuilder.fill(0.05, 0.05, Units.degreesToRadians(30))); // 0.01, 0.01, 10
//0.15, 0.15, 0.5          .75, 0.75, 60

          //1.5 1.5 0.65
          //0.05, 0.05, 10
   
      Field2d m_field = new Field2d();
    
      
            //for characterization
            private boolean isCharacterizing = false;
            private double characterizationVolts = 0.0;
      private Vision vision;

  /** Creates a new DriveSubsystem. */
    public DriveSubsystem() {
      
    this.xLimiter = new SlewRateLimiter(1.4); // was 1.2, then 1.4
    this.yLimiter = new SlewRateLimiter(1.4); // was 1.2, then 1.4
    this.turningLimiter = new SlewRateLimiter(DriveConstants.kTeleDriveMaxAngularSpeedRadiansPerSecond);
    m_VisionLockController.setSetpoint(0);


     m_gyro.setAngleAdjustment(-1);   
    
    zeroHeading();
   
    


     AutoBuilder.configureHolonomic(
      this::getPose,
      this::resetOdometry,
      this::getChassisSpeeds,
      this::setRobotRelativeSpeeds, 
       AutoConstants.autoBuilderPathConfig,
       () -> {var alliance = DriverStation.getAlliance();
        if(alliance.isPresent()){
          return alliance.get() == DriverStation.Alliance.Red;
        }
        return false;},
      this);

    //SmartDashboard.putData("field", m_field);
    vision = new Vision(this);
  }

  @Override

  public void periodic() {
    // Update the odometry in the periodic block
    // TODO: test if this is the thing making it do circles (i.e isCharacterizing == True)
    if(isCharacterizing){
      m_frontLeft.runCharacterization(characterizationVolts, DriveConstants.kFrontLeftChassisAngularOffset);
      m_frontRight.runCharacterization(characterizationVolts, DriveConstants.kFrontRightChassisAngularOffset);
      m_rearLeft.runCharacterization(characterizationVolts, DriveConstants.kBackLeftChassisAngularOffset);
      m_rearRight.runCharacterization(characterizationVolts, DriveConstants.kBackRightChassisAngularOffset);
    }
    
    // Stolen from Isaac, thanks Isaac!!
    //Vision.addFilteredPoseData(getPose(), m_poseEstimator);


    //eventually figure out what each result value is

    if(Math.abs(m_frontLeft.getPosition().distanceMeters) < 1000.0 && Math.abs(m_frontRight.getPosition().distanceMeters) < 1000.0 && Math.abs(m_rearLeft.getPosition().distanceMeters) < 1000.0 && Math.abs(m_rearRight.getPosition().distanceMeters) < 1000.0){
      updateOdometry();
    }

    //if(FieldConstants.kVisionEnable){
    //  vision.UpdateVision();
    //}
    SmartDashboard.putBoolean("Compressor Enabled", Bindings.getCompressorEnabled());
    //SmartDashboard.putBoolean("Limit switch hit", Bindings.getCompressorEnabled());
    //SmartDashboard.putNumber("m_gyro_Get Heading", getHeadingDegrees());
    //SmartDashboard.putNumber("drive angle", getPoseHeading());
    //SmartDashboard.putNumber("target angle 8lue", PoseMath.getTargetAngle(FieldConstants.kSpeakerBackLocation, getPose()).getDegrees());
    //SmartDashboard.putNumber("target angle red", PoseMath.getTargetAngle(FieldConstants.kRedSpeakerBackLocation, getPose()).getDegrees());
    //SmartDashboard.putNumber("Front Left", m_frontLeft.getPosition().distanceMeters);
    //SmartDashboard.putNumber("Front Right", m_frontRight.getPosition().distanceMeters);
    //SmartDashboard.putNumber("Rear Left", m_rearLeft.getPosition().distanceMeters);
    //SmartDashboard.putNumber("Rear Right", m_rearRight.getPosition().distanceMeters);

    //SmartDashboard.putNumber("shooter angle number", LookupTables.getAngleValueAtDistance(PoseMath.getDistanceToSpeakerBack(getPose())));
    //m_field.setRobotPose(getPose());

    
     
      
   



    

  }

  /**
   * Returns the currently-estimated pose of the robot.
   *
   * @return The pose.
   */
  public Pose2d getPose() {
    return m_poseEstimator.getEstimatedPosition();
  }

  public double getPoseHeading()
  {
    return m_poseEstimator.getEstimatedPosition().getRotation().getDegrees();
  }






  /**
   * Resets the odometry to the specified pose.
   *
   * @param pose The pose to which to set the odometry.
   */
  public void resetOdometry(Pose2d pose) {
    m_poseEstimator.resetPosition(
        Rotation2d.fromDegrees(getHeadingDegrees()),
        new SwerveModulePosition[] {
            m_frontLeft.getPosition(),
            m_frontRight.getPosition(),
            m_rearLeft.getPosition(),
            m_rearRight.getPosition()
        },
        pose);
  }


  public void updateOdometry() 
  {
    m_poseEstimator.update(
        Rotation2d.fromDegrees(getHeadingDegrees()),
        new SwerveModulePosition[] {
          m_frontLeft.getPosition(),
          m_frontRight.getPosition(),
          m_rearLeft.getPosition(),
          m_rearRight.getPosition()
        });
  }

  /**
   * Method to drive the robot using joystick info.
   *
   * @param xSpeed        Speed of the robot in the x direction (forward).
   * @param ySpeed        Speed of the robot in the y direction (sideways).
   * @param rot           Angular rate of the robot.
   * @param fieldRelative Whether the provided x and y speeds are relative to the
   *                      field.
   */
  public void drive(double xSpeed, double ySpeed, double rot, boolean fieldRelative) 
  {
    // Adjust input based on max speed
    // xSpeed *= 0.5;
    // ySpeed *= 0.5;
    rot *= 0.5;


    xSpeed = xLimiter.calculate(xSpeed) * DriveConstants.kMaxSpeedMetersPerSecond;
    ySpeed = yLimiter.calculate(ySpeed) * DriveConstants.kMaxSpeedMetersPerSecond;
    rot = turningLimiter.calculate(rot) * DriveConstants.kMaxAngularSpeed;
    double m_HeadingDegrees = getPose().getRotation().getDegrees();
    SwerveModuleState[] swerveModuleStates;
    if(DriverStation.getAlliance().isPresent() && DriverStation.getAlliance().get() == Alliance.Red){
       swerveModuleStates = DriveConstants.kDriveKinematics.toSwerveModuleStates(
        fieldRelative
            ? ChassisSpeeds.discretize(ChassisSpeeds.fromFieldRelativeSpeeds(xSpeed, ySpeed, rot, Rotation2d.fromDegrees(m_HeadingDegrees).rotateBy(Rotation2d.fromDegrees(180))), 0.02)//TODO: changed getHeadingDegrees()
            : ChassisSpeeds.discretize(new ChassisSpeeds(xSpeed, ySpeed, rot), 0.02));
    } else {
      swerveModuleStates = DriveConstants.kDriveKinematics.toSwerveModuleStates(
          fieldRelative
          ? ChassisSpeeds.discretize(ChassisSpeeds.fromFieldRelativeSpeeds(xSpeed, ySpeed, rot, Rotation2d.fromDegrees(m_HeadingDegrees)), 0.02)//TODO: changed getHeadingDegrees()
          : ChassisSpeeds.discretize(new ChassisSpeeds(xSpeed, ySpeed, rot), 0.02));
    }
   
    
    SwerveDriveKinematics.desaturateWheelSpeeds(
        swerveModuleStates, DriveConstants.kMaxSpeedMetersPerSecond);
    m_frontLeft.setDesiredState(swerveModuleStates[0]);
    m_frontRight.setDesiredState(swerveModuleStates[1]);
    m_rearLeft.setDesiredState(swerveModuleStates[2]);
    m_rearRight.setDesiredState(swerveModuleStates[3]);
    
  }






  //removed drive with vision

  public void setModuleDriveVoltage(double voltage) {
    m_frontLeft.setDriveVoltage(voltage);
    m_frontRight.setDriveVoltage(voltage);
    m_rearLeft.setDriveVoltage(voltage);
    m_rearRight.setDriveVoltage(voltage);
    
}

public void setModuleTurnVoltage(double voltage) {
  m_frontLeft.setTurnVoltage(voltage);
  m_frontRight.setTurnVoltage(voltage);
  m_rearLeft.setTurnVoltage(voltage);
  m_rearRight.setTurnVoltage(voltage);
  
}


  /**
   * Sets the wheels into an X formation to prevent movement.
   */
  public void setX() {
    m_frontLeft.setDesiredState(new SwerveModuleState(0, Rotation2d.fromDegrees(45)));
    m_frontRight.setDesiredState(new SwerveModuleState(0, Rotation2d.fromDegrees(-45)));
    m_rearLeft.setDesiredState(new SwerveModuleState(0, Rotation2d.fromDegrees(-45)));
    m_rearRight.setDesiredState(new SwerveModuleState(0, Rotation2d.fromDegrees(45)));
  }



  /**
   * Sets the swerve ModuleStates.
   *
   * @param desiredStates The desired SwerveModule states.
   */
  public void setModuleStates(SwerveModuleState[] desiredStates) {
    SwerveDriveKinematics.desaturateWheelSpeeds(
        desiredStates, DriveConstants.kMaxSpeedMetersPerSecond);
    m_frontLeft.setDesiredState(desiredStates[0]);
    m_frontRight.setDesiredState(desiredStates[1]);
    m_rearLeft.setDesiredState(desiredStates[2]);
    m_rearRight.setDesiredState(desiredStates[3]);
  }

  /** Resets the drive encoders to currently read a position of 0. */
  public void resetEncoders() {
    m_frontLeft.resetEncoders();
    m_rearLeft.resetEncoders();
    m_frontRight.resetEncoders();
    m_rearRight.resetEncoders();
  }

  /** Zeroes the heading of the robot. */
  public void zeroHeading() {
      Pose2d pose;
      if(DriverStation.getAlliance().isPresent()) {
    
          if(DriverStation.getAlliance().get() == Alliance.Red){
           pose = new Pose2d(getEstimatedX(), getEstimatedY(), Rotation2d.fromDegrees(180));
          } else {
           pose = new Pose2d(getEstimatedX(), getEstimatedY(), new Rotation2d(0));
          }
        }  else {
          pose = new Pose2d(getEstimatedX(), getEstimatedY(), new Rotation2d(0));
        }
       m_gyro.zeroYaw();
        m_poseEstimator.resetPosition(
        Rotation2d.fromDegrees(getHeadingDegrees()),
        new SwerveModulePosition[] {
            m_frontLeft.getPosition(),
            m_frontRight.getPosition(),
            m_rearLeft.getPosition(),
            m_rearRight.getPosition()
        },
        pose);
        
    //m_gyro.setAngleAdjustment(-1);
    
  }

  /**
   * Returns the heading of the robot.
   *
   * @return the robot's heading in degrees, from -180 to 180
   */
  public double getHeadingDegrees() {
   // return Rotation2d.fromDegrees(m_gyro.getHeadingDegrees()).getDegrees();
      // FIXME Uncomment if you are using a NavX
    // FIXME Remove if you are using a Pigeon
    //return Rotation2d.fromDegrees(m_pigeon.getFusedHeading());

    // FIXME Uncomment if you are using a NavX
    if (m_gyro.isMagnetometerCalibrated()) {
      //      // We will only get valid fused headings if the magnetometer is calibrated
          // return m_gyro.getFusedHeading() * -1;
          return (m_gyro.getAngle() *-1);
         }
      //
      //    // We have to invert the angle of the NavX so that rotating the robot counter-clockwise makes the angle increase.
         return (m_gyro.getAngle() *-1);
        }



  public Rotation2d getHeadingRotation2d()
  {
    // FIXME Remove if you are using a Pigeon
    //return Rotation2d.fromDegrees(m_pigeon.getFusedHeading());

    // FIXME Uncomment if you are using a NavX
    if (m_gyro.isMagnetometerCalibrated()) {
      //      // We will only get valid fused headings if the magnetometer is calibrated
          // return Rotation2d.fromDegrees(m_gyro.getFusedHeading() * -1);
          return Rotation2d.fromDegrees(m_gyro.getAngle() * -1);
         }
      //
      //    // We have to invert the angle of the NavX so that rotating the robot counter-clockwise makes the angle increase.
         return Rotation2d.fromDegrees(m_gyro.getAngle() * -1);
  }

  public double getRoll()
  {
    return m_gyro.getRoll();
  }

  public double getEstimatedX()
  {
    return m_poseEstimator.getEstimatedPosition().getX();
  }

  public double getEstimatedY()
  {
    return m_poseEstimator.getEstimatedPosition().getY();
  }
  



  /**
   * Returns the turn rate of the robot.
   *
   * @return The turn rate of the robot, in degrees per second
   */
  public double getTurnRate() {
    return m_gyro.getRate() * (DriveConstants.kGyroReversed ? -1.0 : 1.0);
  }

  public MAXSwerveModule[] getMaxSwerveModules()
  {
    MAXSwerveModule[] maxArray = {
      m_frontLeft, 
      m_frontRight, 
      m_rearLeft, 
      m_rearRight};
    return maxArray;
  }

  public SwerveModuleState[] getModuleStates()
  {
    SwerveModuleState[] moduleStates = new SwerveModuleState[4];
    MAXSwerveModule[] modules = getMaxSwerveModules();
    for(int i = 0; i < modules.length; i++)
    {
       moduleStates[i] = modules[i].getState();
    }
    return moduleStates;
  }

  public ChassisSpeeds getChassisSpeeds()
  {
    return Constants.DriveConstants.kDriveKinematics.toChassisSpeeds(getModuleStates());
  }

  public void setRobotRelativeSpeeds(ChassisSpeeds chassisSpeeds)
  {
    var swerveModuleStates = DriveConstants.kDriveKinematics.toSwerveModuleStates(chassisSpeeds);
    SwerveDriveKinematics.desaturateWheelSpeeds(
      swerveModuleStates, DriveConstants.kMaxSpeedMetersPerSecond);
  m_frontLeft.setDesiredState(swerveModuleStates[0]);
  m_frontRight.setDesiredState(swerveModuleStates[1]);
  m_rearLeft.setDesiredState(swerveModuleStates[2]);
  m_rearRight.setDesiredState(swerveModuleStates[3]);
  }

 









  public double getAverageDistanceMeters()
  {
  return m_frontLeft.getEncoderCounts();
  }

  public double getFrontLeftRot()
  {
    return m_frontLeft.getEncoderCounts();
  }

  public double getFrontRightRot()
  {
    return m_frontRight.getEncoderCounts();
  }

  public double getBackLeftRot()
  {
    return m_rearLeft.getEncoderCounts();
  }

  public double getBackRightRot()
  {
    return m_rearRight.getEncoderCounts();
  }

public void runCharacterizationVolts(double volts){
  isCharacterizing = true;
  characterizationVolts = volts;
}

public double getCharacterizationVelocity() {
  double driveVelocityAverage = 0.0;
  driveVelocityAverage += m_frontRight.getCharacterizationVelocity();
  driveVelocityAverage += m_rearRight.getCharacterizationVelocity();
  driveVelocityAverage += m_frontLeft.getCharacterizationVelocity();
  driveVelocityAverage += m_rearRight.getCharacterizationVelocity();
  return driveVelocityAverage / 4.0;

}




public SwerveDrivePoseEstimator getPoseEstimator(){
  return m_poseEstimator;
}
  
}
